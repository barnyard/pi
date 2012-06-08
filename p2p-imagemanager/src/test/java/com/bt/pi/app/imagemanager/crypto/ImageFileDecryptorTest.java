package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.Security;

import javax.crypto.Cipher;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.os.FileManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Cipher.class)
public class ImageFileDecryptorTest {
    private ImageFileDecryptor imageFileDecryptor;
    private Cipher aesCipher;
    @Mock
    private ImageDecompressor imageDecompressor;
    @Mock
    private FileManager fileManager;

    String imagesPath = System.getProperty("java.io.tmpdir");
    String imageProcessingPath = System.getProperty("java.io.tmpdir");

    @Before
    public void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        this.imageFileDecryptor = new ImageFileDecryptor();
        this.imageFileDecryptor.setImagesPath(imagesPath);
        this.imageFileDecryptor.setImageProcessingPath(imageProcessingPath);

        aesCipher = PowerMockito.mock(Cipher.class);
        this.imageFileDecryptor.setImageDecompressor(imageDecompressor);
        this.imageFileDecryptor.setFileManager(fileManager);
    }

    @Test
    public void testDecryptImage() throws Exception {
        // setup
        File encryptedImageFile = File.createTempFile("unittesting", null, new File(imageProcessingPath));
        String data = "ajdfhsajgh sakjfgh sakjgh sa;gjk hsg skjg hsakjg hsajkg hasjg asjkg ";
        FileUtils.writeStringToFile(encryptedImageFile, data);
        encryptedImageFile.deleteOnExit();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        }).when(aesCipher).update((byte[]) anyObject(), anyInt(), anyInt());
        when(aesCipher.doFinal()).thenReturn(new byte[0]);

        File untarredFile = File.createTempFile("unittesting", null);
        FileUtils.writeStringToFile(untarredFile, data);
        when(imageDecompressor.untarImage(isA(String.class), isA(String.class))).thenReturn(untarredFile);

        when(fileManager.copyImage(untarredFile.getAbsolutePath(), imagesPath)).thenReturn(untarredFile.getName());

        // act
        File result = this.imageFileDecryptor.decryptImage(encryptedImageFile, aesCipher);

        // assert
        verify(imageDecompressor).unZipImage(isA(String.class), isA(String.class));
        assertEquals(data, FileUtils.readFileToString(result));
        assertEquals(imagesPath + "/" + untarredFile.getName(), result.getPath());
    }
}

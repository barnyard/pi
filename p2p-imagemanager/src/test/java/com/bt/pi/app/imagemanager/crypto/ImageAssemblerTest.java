package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.sss.client.PisssClient;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class ImageAssemblerTest {
    @InjectMocks
    private ImageAssembler assembler = new ImageAssembler();
    private static final String PART_FILE_0_CONTENT = "hello world!";
    private static final String PART_FILE_1_CONTENT = "how are you?";
    @Mock
    private Manifest manifest;
    @Mock
    private PisssClient pisssClient;
    @Mock
    private User user;
    private String bucketName = "test-bucket";
    private String partName0 = "part-0";
    private String partName1 = "part-1";

    @Before
    public void setUp() throws IOException {
        assembler.setImageProcessingPath(System.getProperty("java.io.tmpdir"));
        File file0 = createPartFile(partName0, PART_FILE_0_CONTENT);
        File file1 = createPartFile(partName1, PART_FILE_1_CONTENT);
        when(pisssClient.getFileFromBucket(eq(bucketName), eq(partName0), eq(user), isA(String.class))).thenReturn(file0);
        when(pisssClient.getFileFromBucket(eq(bucketName), eq(partName1), eq(user), isA(String.class))).thenReturn(file1);
    }

    @Test
    public void shouldAssembleImageWithZeroPartsCorrectly() throws IOException {
        File imageFile = testAssemblePartsForParts();

        // asserts
        assertTrue(imageFile.exists());
        assertEquals(0, imageFile.length());
        FileUtils.deleteQuietly(imageFile);
    }

    @Test
    public void shouldAssembleImageWithOnePartCorrectly() throws IOException {
        File imageFile = testAssemblePartsForParts(partName0);

        // asserts
        assertTrue(imageFile.exists());
        assertEquals(PART_FILE_0_CONTENT, FileUtils.readFileToString(imageFile));
        FileUtils.deleteQuietly(imageFile);
    }

    @Test
    public void shouldAssembleImageWithMoreThanOnePartCorrectly() throws IOException {
        File imageFile = testAssemblePartsForParts(partName0, partName1);

        // asserts
        assertTrue(imageFile.exists());
        assertEquals(PART_FILE_0_CONTENT + PART_FILE_1_CONTENT, FileUtils.readFileToString(imageFile));
        FileUtils.deleteQuietly(imageFile);
    }

    @Test(expected = BucketObjectNotFoundException.class)
    public void shouldNotAssembleImageWhenPartFileCannotBeFound() throws IOException {
        String partName = "non-existent-part";
        when(this.pisssClient.getFileFromBucket(eq(bucketName), eq(partName), eq(user), isA(String.class))).thenThrow(new BucketObjectNotFoundException());
        testAssemblePartsForParts(partName);
    }

    private File testAssemblePartsForParts(String... partNames) throws IOException {
        setImageAssemblyExpectationsForParts(partNames);

        return assembler.assembleParts(manifest, bucketName, user);
    }

    private void setImageAssemblyExpectationsForParts(String... partNames) {
        List<String> partNamesList = getPartNamesList(partNames);
        when(manifest.getPartFilenames()).thenReturn(partNamesList);
    }

    private List<String> getPartNamesList(String... partNames) {
        List<String> partNamesList = new ArrayList<String>();
        for (String partName : partNames) {
            partNamesList.add(partName);
        }
        return partNamesList;
    }

    private File createPartFile(String name, String content) throws IOException {
        File partFile = File.createTempFile("unittesting", null);
        partFile.deleteOnExit();
        FileUtils.writeStringToFile(partFile, content);
        return partFile;
    }
}

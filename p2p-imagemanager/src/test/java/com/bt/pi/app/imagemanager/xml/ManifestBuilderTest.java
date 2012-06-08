package com.bt.pi.app.imagemanager.xml;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class ManifestBuilderTest {

    private static final String VALID_XML_DOCUMENT_FILENAME = "manifest.xml";
    private static final String ENCRYPTED_KEY = "671c7d6b719b1cdeac";
    private static final String ENCRYPTED_IV = "1a355c31d4c37d17739";
    private static final String ARCH = "i386";
    private static final String KERNEL_ID = "k123";
    private static final String RAMDISK_ID = "r123";
    private static final String SIGNATURE = "4adaf84301a4d1a79969d4941e0acc7d3e4a1ac953a9651e8c6c62db5a463b707ef5b49733605c7425c826586b2bd0efa67fb71d979b3d8384c6e45493d78e564106fcf9e887d7f639152323e735fd3c7e5ef662f41c2ff3f395bf2836fef3feba1080f3276aaad8d91c139df760b72c72dbcb7f11e362ce5c6206e14a202ed6214a2e3e791ece25b4c4e3fd57bcc72d78625c565d652d581d880fafcd2ed0ef88d5c3a8c2c913443ff1aa66e362bfc0661c34458eff50bc42827fb8005bd6d9c609eac76a37d7f7c586077c9e0434964b090dd84d8428287ca72c830c17cced71940f3c7c7ba5b05b685826620220c49ca52d767febbaab9f5e26d0effdd38b";
    private static final String MACHINE_CONFIGURATION = "<machine_configuration><ramdisk_id>" + RAMDISK_ID + "</ramdisk_id><kernel_id>" + KERNEL_ID + "</kernel_id><architecture>" + ARCH + "</architecture></machine_configuration>";
    private static final String IMAGE = "<image><name>test-file</name><ec2_encrypted_key algorithm='AES-128-CBC'>671c7d6b719b1cdeac</ec2_encrypted_key><ec2_encrypted_iv>1a355c31d4c37d17739</ec2_encrypted_iv><parts count='2'><part index='0'><filename>test-file.part.0</filename></part><part index='1'><filename>test-file.part.1</filename></part></parts></image>";
    private static final String PART_FILENAME_0 = "test-file.part.0";
    private static final String PART_FILENAME_1 = "test-file.part.1";
    private ManifestBuilder builder;
    private ManifestBuilder builderWithMocks;
    private XMLParser mockParser;
    private XPathEvaluator mockEvaluator;
    private File mockFile;
    private Document mockDocument;

    @Before
    public void setUp() {
        XMLParser parser = new XMLParser();
        XPathEvaluator evaluator = new XPathEvaluator();
        builder = new ManifestBuilder();
        builder.setParser(parser);
        builder.setEvaluator(evaluator);

        mockParser = mock(XMLParser.class);
        mockEvaluator = mock(XPathEvaluator.class);
        builderWithMocks = new ManifestBuilder();
        builderWithMocks.setParser(mockParser);
        builderWithMocks.setEvaluator(mockEvaluator);

        mockFile = mock(File.class);
        mockDocument = mock(Document.class);
    }

    @Test
    public void shouldBuildAManifestGivenAValidManifestFile() throws IOException {
        Manifest manifest = buildDefaultManifest();
        File file = getManifestFile();

        assertEquals(manifest, builder.build(file));
    }

    @Test
    public void shouldBuildAManifestGivenAValidManifestXmlString() throws IOException {
        Manifest manifest = buildDefaultManifest();
        File file = getManifestFile();
        String manifestXml = FileUtils.readFileToString(file);

        assertEquals(manifest, builder.build(manifestXml));
    }

    @Test(expected = ParseException.class)
    public void shouldThrowAParseExceptionWhenManifestFileCannotBeParsed() throws IOException {
        when(mockParser.parse(mockFile)).thenThrow(new ParseException());

        builderWithMocks.build(mockFile);
    }

    @Test(expected = ParseException.class)
    public void shouldThrowAParseExceptionWhenAValueCannotBeRetrievedFromTheManifestFile() throws IOException {
        when(mockParser.parse(mockFile)).thenReturn(mockDocument);
        when(mockEvaluator.getValue(anyString(), eq(mockDocument))).thenThrow(new ParseException());

        builderWithMocks.build(mockFile);
    }

    @Test(expected = ParseException.class)
    public void shouldThrowAParseExceptionWhenAnXmlFragmentCannotBeRetrievedFromTheManifestFile() throws IOException {
        when(mockParser.parse(mockFile)).thenReturn(mockDocument);
        when(mockEvaluator.getXMLFragment(anyString(), eq(mockDocument))).thenThrow(new ParseException());

        builderWithMocks.build(mockFile);
    }

    @Test(expected = ParseException.class)
    public void shouldThrowAParseExceptionWhenAListOfValuesCannotBeRetrievedFromTheManifestFile() throws IOException {
        when(mockParser.parse(mockFile)).thenReturn(mockDocument);
        when(mockEvaluator.getValues(anyString(), eq(mockDocument))).thenThrow(new ParseException());

        builderWithMocks.build(mockFile);
    }

    private Manifest buildDefaultManifest() {
        List<String> partFilenames = new ArrayList<String>();
        partFilenames.add(PART_FILENAME_0);
        partFilenames.add(PART_FILENAME_1);
        return new Manifest(ENCRYPTED_KEY, ENCRYPTED_IV, SIGNATURE, MACHINE_CONFIGURATION, IMAGE, partFilenames, ARCH, KERNEL_ID, RAMDISK_ID);
    }

    private File getManifestFile() {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(VALID_XML_DOCUMENT_FILENAME);
        return new File(fileUrl.getPath());
    }
}

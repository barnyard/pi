package com.bt.pi.app.imagemanager.xml;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class XMLParserTest {

    private XMLParser parser;

    @Before
    public void setUp() {
        parser = new XMLParser();
    }

    @Test
    public void shouldReturnADocumentWhenGivenAValidXmlFileToParse() {
        assertNotNull(getDocument("valid.xml"));
    }

    @Test
    public void shouldReturnADocumentWhenGivenAValidXmlStringToParse() throws IOException {
        assertNotNull(FileUtils.readFileToString(getXmlFile("valid.xml")));
    }

    @Test(expected = ParseException.class)
    public void shouldThrownAParseExceptionWhenGivenAnInvalidXmlFileToParse() {
        getDocument("invalid.xml");
    }

    @Test(expected = ParseException.class)
    public void shouldThrowAParseExceptionWhenGivenANonExistentFileToParse() {
        File file = new File("non-existent-file.xml");

        parser.parse(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnIllegalArgumentExceptionWhenGivenANullFileToParse() {
        parser.parse((File) null);
    }

    private Document getDocument(String filename) {
        File file = getXmlFile(filename);
        return parser.parse(file);
    }

    private File getXmlFile(String filename) {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(filename);
        return new File(fileUrl.getPath());
    }
}

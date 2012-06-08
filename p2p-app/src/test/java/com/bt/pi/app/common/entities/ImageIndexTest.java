package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class ImageIndexTest {

    private KoalaJsonParser koalaJsonParser;

    @Before
    public void before() {
        koalaJsonParser = new KoalaJsonParser();
    }

    @Test
    public void testRoundTrip() {
        // setup
        ImageIndex imageIndex = new ImageIndex();
        imageIndex.getImages().add("imageId1");
        imageIndex.getImages().add("imageId2");

        // act
        String json = koalaJsonParser.getJson(imageIndex);
        ImageIndex reverse = (ImageIndex) koalaJsonParser.getObject(json, ImageIndex.class);

        // assert
        assertEquals(imageIndex.getImages().size(), reverse.getImages().size());
        assertTrue(reverse.getImages().contains("imageId1"));
        assertTrue(reverse.getImages().contains("imageId2"));
    }

    @Test
    public void testMigrate() throws Exception {
        // setup
        String json = "{" + "\"type\" : \"ImageIndex\"," + "\"map\" : {" + "\"imageId2\" : {" + "\"state\" : \"PENDING\"," + "\"type\" : \"Image\"," + "\"public\" : true," + "\"url\" : \"img:imageId2\"," + "\"machineType\" : \"MACHINE\","
                + "\"architecture\" : null," + "\"platform\" : \"linux\"," + "\"imageId\" : \"imageId2\"," + "\"kernelId\" : null," + "\"manifestLocation\" : null," + "\"ownerId\" : null," + "\"ramdiskId\" : null," + "\"version\" : 0" + "},"
                + "\"imageId1\" : {" + "\"state\" : \"PENDING\"," + "\"type\" : \"Image\"," + "\"public\" : false," + "\"url\" : \"img:imageId1\"," + "\"machineType\" : \"MACHINE\"," + "\"architecture\" : null," + "\"platform\" : \"linux\","
                + "\"imageId\" : \"imageId1\"," + "\"kernelId\" : null," + "\"manifestLocation\" : null," + "\"ownerId\" : null," + "\"ramdiskId\" : null," + "\"version\" : 0" + "}" + "}," + "\"url\" : \"idx:images\"," + "\"version\" : 0" + "}";

        // act
        ImageIndex reverse = (ImageIndex) koalaJsonParser.getObject(json, ImageIndex.class);
        String json1 = koalaJsonParser.getJson(reverse);
        System.out.println(json1);

        // assert
        assertEquals(2, reverse.getImages().size());
        assertTrue(reverse.getImages().contains("imageId1"));
        assertTrue(reverse.getImages().contains("imageId2"));

        // act
        ImageIndex reverse1 = (ImageIndex) koalaJsonParser.getObject(json1, ImageIndex.class);

        // assert
        assertEquals(2, reverse1.getImages().size());
        assertTrue(reverse1.getImages().contains("imageId1"));
        assertTrue(reverse1.getImages().contains("imageId2"));
    }
}

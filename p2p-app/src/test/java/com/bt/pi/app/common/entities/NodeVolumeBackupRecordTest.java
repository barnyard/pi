package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class NodeVolumeBackupRecordTest {
    @Test
    public void shouldSetProperties() {
        // setup
        NodeVolumeBackupRecord volumeBackup = new NodeVolumeBackupRecord();
        volumeBackup.setNodeId("1234");
        volumeBackup.setLastBackup(1234234);

        // assert
        assertEquals("1234", volumeBackup.getNodeId());
        assertEquals(1234234, volumeBackup.getLastBackup());
    }

    @Test
    public void shouldRoundtripJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
        NodeVolumeBackupRecord volumeBackup = new NodeVolumeBackupRecord("1234");
        volumeBackup.setLastBackup(1234);

        // act
        String json = koalaJsonParser.getJson(volumeBackup);
        NodeVolumeBackupRecord reverse = (NodeVolumeBackupRecord) koalaJsonParser.getObject(json, NodeVolumeBackupRecord.class);

        // assert
        assertEquals(volumeBackup, reverse);
    }
}

package com.bt.pi.app.instancemanager.testing;

import com.bt.pi.app.common.os.FileManager;

public class StubFileManager extends FileManager {
    @Override
    public boolean fileExists(String filePath) {
        if (filePath.contains("pmi-ApplePie") || filePath.contains("pki-PumpkinPie") || filePath.contains("pri-RhubarbPie"))
            return false;
        if (filePath.contains("-terminated-"))
            return false;
        return true;
    }
}

/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

public class CertificateUtils {

    public CertificateUtils(){
    }
    
    public boolean areEqual(byte[] bs1, byte[] bs2) {
        if (null == bs1 || null == bs2)
            return false;
        if (bs1.length != bs2.length)
            return false;
        
        int length = bs1.length;
        for (int i = 0; i < length; i++){
            if (bs1[i] != bs2[i])
                return false;
        }
        
        return true;
    }

}

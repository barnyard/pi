package com.bt.nia.koala.robustness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
	private static final int BUFSIZE = 4096;

	public static void unzipFile(String zipFile, String extDir) {
		File f = new File(zipFile);
		try {
			FileInputStream fi = new FileInputStream(f);
			ZipInputStream zipInput = new ZipInputStream(fi);
			ZipEntry zip = zipInput.getNextEntry();
			while (zip != null) {
				File fo = new File(extDir + "/" + zip.getName());
				FileOutputStream fout = new FileOutputStream(fo);
				byte inbuf[] = new byte[BUFSIZE];
				int n = 0;
				while ((n = zipInput.read(inbuf, 0, BUFSIZE)) != -1) {
					fout.write(inbuf, 0, n);
				}
				fout.close();
				zip = zipInput.getNextEntry();
			}
			zipInput.close();
			fi.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

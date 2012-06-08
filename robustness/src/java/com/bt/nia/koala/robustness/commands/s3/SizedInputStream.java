/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness.commands.s3;

import java.io.IOException;
import java.io.InputStream;

public class SizedInputStream extends InputStream {
	private long size;

	public SizedInputStream(long bytes) {
		this.size = bytes;
	}

	@Override
	public int read() throws IOException {
		this.size--;
		if (size < 0)
			return -1;
		return 0;
	}

}

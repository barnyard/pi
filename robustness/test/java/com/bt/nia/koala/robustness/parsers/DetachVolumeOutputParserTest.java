package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DetachVolumeOutputParserTest {
	private DetachVolumeOutputParser detachVolumeOutputParser;
	private List<String> outputLines;
	private String detachVolumeOutput;
	private String piDetachVolumeOutput;

	@Before
	public void before() {
		detachVolumeOutputParser = new DetachVolumeOutputParser();

		outputLines = new ArrayList<String>();

		detachVolumeOutput = "ATTACHMENT      vol-2DCF0465    i-4D930980      sdb             2009-05-15T16:29:00+0000";
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		detachVolumeOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");

		// act
		detachVolumeOutputParser.parse(outputLines);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		outputLines.add(detachVolumeOutput);

		// act
		String[] res = detachVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("vol-2DCF0465", res[0]);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceIdEucalyptus152() {
		detachVolumeOutput = "ATTACHMENT  vol-BA3D0A24    i-397B061B      sdb     attached        2009-07-03T14:51:27+0000";
		outputLines.add(detachVolumeOutput);

		// act
		String[] res = detachVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("vol-BA3D0A24", res[0]);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceIdPi() {
		detachVolumeOutput = "ATTACHMENT      vol-FF315ED8    i-5A4D3CAB      /dev/sdb        detaching       2010-03-30T18:09:00+0000";
		outputLines.add(detachVolumeOutput);

		// act
		String[] res = detachVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("vol-FF315ED8", res[0]);
	}
}

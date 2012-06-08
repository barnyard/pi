package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class CreateVolumeOutputParserTest {
	private CreateVolumeOutputParser createVolumeOutputParser;
	private List<String> outputLines;
	private String createVolumeOutput;

	@Before
	public void before() {
		createVolumeOutputParser = new CreateVolumeOutputParser("playpen");

		outputLines = new ArrayList<String>();

		createVolumeOutput = "VOLUME  vol-2DCF0465    1                       creating        2009-05-15T16:21:22+0000 ";
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		createVolumeOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");

		// act
		createVolumeOutputParser.parse(outputLines);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(createVolumeOutput);

		// act
		String[] res = createVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("vol-2DCF0465", res[0]);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceIdEucalyptus152() {
		// setup
		createVolumeOutput = "VOLUME  vol-BA380A20    1               playpen creating        2009-07-03T13:37:00+0000 ";
		outputLines.add(createVolumeOutput);

		// act
		try {
			String[] res = createVolumeOutputParser.parse(outputLines);

			// assert
			assertEquals(1, res.length);
			assertEquals("vol-BA380A20", res[0]);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}

	}

	@Test
	public void shouldDetectPendingAndReturnInstanceIdForPiOutput() {
		// setup
		CreateVolumeOutputParser parser = new CreateVolumeOutputParser("Chigorin");

		createVolumeOutput = "VOLUME     vol-2534DDDD    1               Chigorin        creating 2010-03-22T15:37:38+0000";
		outputLines.add(createVolumeOutput);

		// act
		try {
			String[] res = parser.parse(outputLines);

			// assert
			assertEquals(1, res.length);
			assertEquals("vol-2534DDDD", res[0]);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void shouldDetectVolAndReturnStateWhenSnapshotIdIsOnPiOutput() {
		// setup
		createVolumeOutput = "VOLUME	vol-000AKMKI	0	snap-000HU7Ri	cube1-1a	creating	2010-11-17T14:59:19+0000";
		outputLines.add(createVolumeOutput);

		createVolumeOutputParser = new CreateVolumeOutputParser("cube1-1a", true);

		// act
		String[] res = createVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("vol-000AKMKI", res[0]);
	}
}

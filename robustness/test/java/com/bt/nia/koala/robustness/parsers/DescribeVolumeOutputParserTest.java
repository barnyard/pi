package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeVolumeOutputParserTest {
	private DescribeVolumeOutputParser describeVolumeOutputParser;
	private List<String> outputLines;
	private String describeVolumeVolOutput;
	private String describeVolumeAttOutput;

	@Before
	public void before() {
		describeVolumeOutputParser = new DescribeVolumeOutputParser("playpen");

		outputLines = new ArrayList<String>();

		describeVolumeVolOutput = "VOLUME  vol-2DCF0465    1                       available       2009-05-15T16:21:22+0000";
		describeVolumeAttOutput = "ATTACHMENT      vol-2DCF0465    i-4D930980      sdb             2009-05-15T16:27:07+0000";
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		describeVolumeOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");

		// act
		describeVolumeOutputParser.parse(outputLines);
	}

	@Test
	public void shouldDetectVolAndReturnState() {
		// setup
		outputLines.add(describeVolumeVolOutput);

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("available", res[0]);
	}

	@Test
	public void shouldDetectVolAndReturnStateEucalyptus152() {
		// setup
		describeVolumeVolOutput = "VOLUME  vol-BA380A20    1               playpen available        2009-07-03T13:37:00+0000";
		outputLines.add(describeVolumeVolOutput);

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("available", res[0]);
	}

	@Test
	public void shouldDetectVolAndReturnStateWhenBothVolAndAttLine() {
		// setup
		outputLines.add(describeVolumeVolOutput);
		outputLines.add(describeVolumeAttOutput);

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(2, res.length);
		assertEquals("available", res[0]);
		assertEquals("sdb", res[1]);
	}

	@Test
	public void shouldDetectVolAndReturnStateWhenBothVolAndAttLineEucalyptus152() {
		// setup
		describeVolumeVolOutput = "VOLUME  vol-BA380A20    1               playpen available        2009-07-03T13:37:00+0000";
		describeVolumeAttOutput = "ATTACHMENT      vol-BA3E0A22    i-4737078C      sdb     attached        2009-07-03T14:55:44+0000";
		outputLines.add(describeVolumeVolOutput);
		outputLines.add(describeVolumeAttOutput);

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(2, res.length);
		assertEquals("available", res[0]);
		assertEquals("sdb", res[1]);
	}

	@Test
	public void shouldDetectVolAndReturnStateWhenBothVolAndAttachingLineEucalyptus152() {
		// setup
		describeVolumeVolOutput = "VOLUME  vol-BA380A20    1               playpen available        2009-07-03T13:37:00+0000";
		describeVolumeAttOutput = "ATTACHMENT      vol-BA3E0A22    i-4737078C      sdb     attaching        2009-07-03T14:55:44+0000";
		outputLines.add(describeVolumeVolOutput);
		outputLines.add(describeVolumeAttOutput);

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(2, res.length);
		assertEquals("available", res[0]);
		assertEquals("sdb", res[1]);
	}

	@Test
	public void shouldDetectVolAndReturnStateWhenBothVolAndAttachingLineOnPiOutput() {
		// setup
		describeVolumeVolOutput = "VOLUME  vol-19CF1135    1               Chigorin        available 2010-03-22T16:52:20+0000";
		outputLines.add(describeVolumeVolOutput);

		describeVolumeOutputParser = new DescribeVolumeOutputParser("Chigorin");

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("available", res[0]);
	}

	@Test
	public void shouldDetectVolAndReturnStateWhenSnapshotIdIsOnPiOutput() {
		// setup
		describeVolumeVolOutput = "VOLUME	vol-000AKMKI	0	snap-000HU7Ri	cube1-1a	creating	2010-11-17T14:59:19+0000";
		outputLines.add(describeVolumeVolOutput);

		describeVolumeOutputParser = new DescribeVolumeOutputParser("cube1-1a", true);

		// act
		String[] res = describeVolumeOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("creating", res[0]);
	}
}

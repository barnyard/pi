package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DescribeImageOutputParserTest {
	protected DescribeImageOutputParser describeImageOutputParser;
	protected List<String> outputLines;
	private String piImageOutput;

	@Before
	public void before() {
		describeImageOutputParser = new DescribeImageOutputParser();

		outputLines = new ArrayList<String>();

	}

	@Test
	public void shouldDetectDescribeImageOutputIfImageIsPending() {
		// setup
		piImageOutput = "IMAGE   pmi-6F789354    robustness-image-bucket/ttylinux.img.manifest.xml       robustnessdev   PENDING private         x86_64  MACHINE       pki-ADABAAAD            linux";
		outputLines.add(piImageOutput);

		// act
		String[] res = describeImageOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("PENDING", res[0]);
	}

	@Test
	public void shouldDetectDescribeImageOutputIfImageIsAvailable() {
		// setup
		piImageOutput = "IMAGE   pmi-6F789354    robustness-image-bucket/ttylinux.img.manifest.xml       robustnessdev   AVAILABLE private         x86_64  MACHINE       pki-ADABAAAD            linux";
		outputLines.add(piImageOutput);

		// act
		String[] res = describeImageOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("AVAILABLE", res[0]);
	}

	@Test
	public void shouldDetectDescribeImageOutputWithAnyUser() {
		// setup
		piImageOutput = "IMAGE	pmi-03873A85	robustness-image-bucket/ttylinux.img.manifest.xml	robustness	AVAILABLE	private		x86_64	MACHINE	pki-B01C518D		linux";
		outputLines.add(piImageOutput);

		// act
		String[] res = describeImageOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("AVAILABLE", res[0]);
	}

	@Test
	public void shouldDetectDescribeImageOutputForKernel() {
		// setup
		piImageOutput = "IMAGE	pki-474efa75	robustnessdbdd4308-d1de-4f5d-a44c-501c874f35a1/vmlinuz-2.6.16.33-xen.manifest.xml	robustnessbuild	AVAILABLE	private		x86_64	KERNEL			linux";
		outputLines.add(piImageOutput);

		// act
		String[] res = describeImageOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("AVAILABLE", res[0]);
	}
}

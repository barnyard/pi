package com.bt.pi.api;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class Ec2ToolsSuiteTest {

    public static Test suite() {
        TestSuite ts = new TestSuite();
        ts.addTestSuite(Ec2RightScaleTestCase.class);
        ts.addTestSuite(Ec2ApiToolsTestCase.class);
        ts.addTestSuite(Ec2TimKayTestCase.class);
        ts.addTestSuite(Ec2TypicaTestCase.class);
        return new Ec2Setup(ts);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

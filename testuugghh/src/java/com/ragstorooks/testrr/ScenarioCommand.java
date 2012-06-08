package com.ragstorooks.testrr;

import java.util.concurrent.CountDownLatch;

public interface ScenarioCommand extends Runnable {
	long getDelayMillis();
	CountDownLatch isDone();
}

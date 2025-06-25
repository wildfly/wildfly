/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.suspend;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * <p>Batchlet that runs until the static method <em>success</em> is called.
 * It is used to perform the suspend and resume while this job is running.
 * Only one job of this class can be executed simultaneously.</p>
 *
 * @author rmartinc
 */
@Named
public class LongRunningBatchlet implements Batchlet {

    private static final int STOPPED = 0;
    private static final int STARTED = 1;
    private static final int COMPLETED = 2;

    @Inject
    @BatchProperty(name = "max.seconds")
    private Integer maxSeconds;

    private static final AtomicInteger STATE = new AtomicInteger(STOPPED);

    public static boolean isStarted() {
        return STATE.get() == STARTED;
    }

    public static void success() throws Exception {
        if (!STATE.compareAndSet(STARTED, COMPLETED)) {
            throw new AssertionError("An attempt was made to complete a job that has not been started.");
        }
    }

    @Override
    public String process() throws Exception {
        if (!STATE.compareAndSet(STOPPED, STARTED)) {
            throw new AssertionError("Cannot start a job that is not in the stopped state.");
        }
        final long timeout = TimeUnit.SECONDS.toMillis(maxSeconds);
        final long start = System.currentTimeMillis();
        long elapsed = 0;
        while (elapsed < timeout) {
            final int current = STATE.get();
            if (current == COMPLETED) {
                return "OK";
            }
            if (current == STOPPED) {
                return "STOPPED";
            }
            TimeUnit.MILLISECONDS.sleep(50L);
            elapsed = System.currentTimeMillis() - start;
        }
        return "TIMEOUT";
    }

    @Override
    public void stop() throws Exception {
        STATE.set(STOPPED);
    }
}

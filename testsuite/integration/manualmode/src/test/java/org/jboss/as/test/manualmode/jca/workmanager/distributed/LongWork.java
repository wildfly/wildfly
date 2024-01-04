/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import jakarta.resource.spi.work.DistributableWork;
import java.io.Serializable;

public class LongWork implements DistributableWork, Serializable {

    private boolean quit = false;

    /**
     * Note that some distributed workmanager threads may depend on the exact time long work takes.
     */
    public static final long WORK_TIMEOUT = 1000L; // 1 second
    public static final long SLEEP_STEP = 50L; // 0.05 seconds

    @Override
    public void release() {
        quit = true;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long finishTime = startTime + WORK_TIMEOUT;

        while (!quit) {
            try {
                Thread.sleep(SLEEP_STEP);
            } catch (InterruptedException ignored) {
                // ignored
            }
            if (System.currentTimeMillis() >= finishTime) quit = true;
        }
    }
}

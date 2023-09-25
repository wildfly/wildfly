/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.batch.stoprestart;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@Dependent
public class Batchlet1 implements Batchlet {
    private final AtomicBoolean stopRequested = new AtomicBoolean();

    @Inject
    @BatchProperty
    long seconds;

    @Inject
    @BatchProperty
    int interval;

    @Override
    public String process() throws Exception {
        if (seconds > 0) {
            long startTime = System.currentTimeMillis();
            long targetDuration = seconds * 1000;
            long sleepAmount;
            while((sleepAmount = System.currentTimeMillis() - startTime) < targetDuration && !stopRequested.get()) {
                Thread.sleep(interval);
            }
            return "Slept " + TimeUnit.MILLISECONDS.toSeconds(sleepAmount) + " seconds";
        }
        return "Direct return no sleep";
    }

    @Override
    public void stop() throws Exception {
        stopRequested.set(true);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction.inflow;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.resource.spi.work.WorkEvent;
import jakarta.resource.spi.work.WorkListener;

/**
 * Simple listener to monitor if work was done already.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowWorkListener implements WorkListener {
    private AtomicBoolean isAccepted = new AtomicBoolean(false);
    private AtomicBoolean isRejected = new AtomicBoolean(false);
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private AtomicBoolean isCompleted = new AtomicBoolean(false);

    @Override
    public void workAccepted(WorkEvent e) {
        isAccepted.set(true);
    }

    @Override
    public void workRejected(WorkEvent e) {
        isRejected.set(true);
    }

    @Override
    public void workStarted(WorkEvent e) {
        isStarted.set(true);
    }

    @Override
    public void workCompleted(WorkEvent e) {
        isCompleted.set(true);
    }

    boolean isAccepted() {
        return isAccepted.get();
    }

    boolean isRejected() {
        return isRejected.get();
    }

    boolean isStarted() {
        return isStarted.get();
    }

    boolean isCompleted() {
        return isCompleted.get();
    }
}

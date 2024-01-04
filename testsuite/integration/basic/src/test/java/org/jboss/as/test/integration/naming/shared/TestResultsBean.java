/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.shared;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Martins
 */
@Startup
@Singleton
public class TestResultsBean implements TestResults {

    private final CountDownLatch latch = new CountDownLatch(4);

    private boolean postContructOne;
    private boolean postContructTwo;
    private boolean preDestroyOne;
    private boolean preDestroyTwo;

    public boolean isPostContructOne() {
        return postContructOne;
    }

    public void setPostContructOne(boolean postContructOne) {
        this.postContructOne = postContructOne;
        latch.countDown();
    }

    public boolean isPostContructTwo() {
        return postContructTwo;
    }

    public void setPostContructTwo(boolean postContructTwo) {
        this.postContructTwo = postContructTwo;
        latch.countDown();
    }

    public boolean isPreDestroyOne() {
        return preDestroyOne;
    }

    public void setPreDestroyOne(boolean preDestroyOne) {
        this.preDestroyOne = preDestroyOne;
        latch.countDown();
    }

    public boolean isPreDestroyTwo() {
        return preDestroyTwo;
    }

    public void setPreDestroyTwo(boolean preDestroyTwo) {
        this.preDestroyTwo = preDestroyTwo;
        latch.countDown();
    }

    public void await(long timeout, TimeUnit timeUnit) throws InterruptedException {
        latch.await(timeout, timeUnit);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AppClientStateSingleton implements AppClientSingletonRemote {
    private static final Logger logger = Logger.getLogger("org.jboss.as.test.appclient");

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private volatile String value;

    @Override
    public void reset() {
        logger.trace("Reset called!");
        value = null;
        //if we have a thread blocked on the latch release it
        latch.countDown();
        latch = new CountDownLatch(1);
    }

    @Override
    public void makeAppClientCall(final String value) {
        logger.trace("AppClient Call called!");
        this.value = value;
        latch.countDown();
    }

    @Override
    public String awaitAppClientCall() {
        try {
            boolean b = latch.await(30, TimeUnit.SECONDS);
            logger.trace("Await returned: " + b + " : " + value);
            if (!b) {
                ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                for (ThreadInfo info : threadInfos) {
                    logger.trace(info);
                }
            }
            return value;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String echo(String toEcho) {
        return toEcho;
    }
}

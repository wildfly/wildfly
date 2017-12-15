/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;

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
}

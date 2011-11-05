/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.timerservice.expired;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TimerConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class ExpiredTimerTestCase {

    private static final Logger logger = Logger.getLogger(ExpiredTimerTestCase.class);

    @EJB (mappedName = "java:module/SingletonBean")
    private SingletonBean bean;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "expired-timer-test.jar");
        ejbJar.addPackage(ExpiredTimerTestCase.class.getPackage());
        return ejbJar;
    }

    @Test
    public void testInvocationOnExpiredTimer() throws Exception {
        final long twoSecondsFromNow = 2000;
        final CountDownLatch timeoutNotifier = new CountDownLatch(1);
        this.bean.createSingleActionTimer(twoSecondsFromNow, new TimerConfig(null, false), timeoutNotifier);
        // wait for the timeout to be invoked
        final boolean timeoutInvoked = timeoutNotifier.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Timeout method was not invoked (within 5 seconds)", timeoutInvoked);
        try {
            this.bean.invokeOnExpiredTimer();
            Assert.fail("Expected to fail on invoking on an expired timer");
        } catch (NoSuchObjectLocalException nsole) {
            // expected
            logger.info("Got the expected exception " + nsole);
        }
    }
}


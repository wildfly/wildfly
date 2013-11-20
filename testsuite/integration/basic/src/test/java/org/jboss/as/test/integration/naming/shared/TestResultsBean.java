/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.naming.shared;

import javax.ejb.Singleton;
import javax.ejb.Startup;
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

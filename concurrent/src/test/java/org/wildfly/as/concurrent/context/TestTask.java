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

package org.wildfly.as.concurrent.context;

import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * @author Eduardo Martins
 */
public class TestTask {

    public TestTask() {
        this(1);
    }

    public TestTask(int executions) {
        done = new CountDownLatch(executions);
    }

    TestContext.SetData setData;
    final CountDownLatch done;
    public Runnable innerTask;

    public void setInnerTask(Runnable innerTask) {
        this.innerTask = innerTask;
    }

    public void execute() {
        setData = TestContext.getCurrent();
        try {
            if (innerTask != null) {
                innerTask.run();
            }
        } finally {
            if (done.getCount() != 0) {
                done.countDown();
            } else {
                throw new RuntimeException();
            }
        }
    }

    public void assertContextIsNotSet() {
        for (TestContext.SetData setData : TestContext.allContexts.keySet()) {
            if (setData.testContext.getObject() == this) {
                fail();
            }
        }
    }

    public void assertContextWasSet() throws InterruptedException {
        assertContextWasSet(true);
    }

    public void assertContextWasSet(boolean wait) throws InterruptedException {
        if (wait) {
            if (!this.done.await(10, TimeUnit.SECONDS)) {
                fail();
            }
        }
        Assert.assertTrue(this.setData != null && this.setData.testContext.getObject() == this);
    }

    public void assertContextWasReset() throws InterruptedException {
        if (!this.setData.countDownLatch.await(10, TimeUnit.SECONDS)) {
            fail();
        }
    }

}

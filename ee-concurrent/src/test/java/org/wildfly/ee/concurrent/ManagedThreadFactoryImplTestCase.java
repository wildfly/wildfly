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
package org.wildfly.ee.concurrent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.concurrent.ManageableThread;

/**
 *
 */
public class ManagedThreadFactoryImplTestCase {

    protected ManagedThreadFactoryImpl threadFactory;

    protected ManagedThreadFactoryImpl newManagedThreadFactory(ContextConfiguration contextConfiguration) {
        return new ManagedThreadFactoryImpl(contextConfiguration);
    }

    @Before
    public void beforeTest() {
        final TestContextConfiguration contextConfiguration = new TestContextConfiguration();
        threadFactory = newManagedThreadFactory(contextConfiguration);
        Assert.assertFalse(threadFactory.isShutdown());
        Assert.assertTrue(threadFactory.getThreads().isEmpty());
        Assert.assertTrue(TestContextImpl.allContexts.isEmpty());
    }

    @Test
    public void testRun() throws Exception {
        TestRunnable runnable = new TestRunnable();
        runnable.assertContextIsNotSet();
        Thread thread = threadFactory.newThread(runnable);
        Assert.assertFalse(((ManageableThread) thread).isShutdown());
        Assert.assertEquals(1, threadFactory.getThreads().size());
        Assert.assertEquals(thread, threadFactory.getThreads().get(0));
        thread.start();
        runnable.assertContextWasSet();
        runnable.assertContextWasReset();
    }

    @Test
    public void testInterrupt() throws Exception {
        TestRunnable runnable = new TestRunnable();
        runnable.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        };
        runnable.assertContextIsNotSet();
        Thread thread = threadFactory.newThread(runnable);
        Assert.assertFalse(((ManageableThread) thread).isShutdown());
        Assert.assertEquals(1, threadFactory.getThreads().size());
        Assert.assertEquals(thread, threadFactory.getThreads().get(0));
        thread.start();
        try {
            Thread.sleep(1000);
            thread.interrupt();
        } catch (InterruptedException e) {
            // ignore
        }
        runnable.assertContextWasSet();
        runnable.assertContextWasReset();
    }

    @After
    public void afterTest() {
        threadFactory.shutdown();
        Assert.assertTrue(threadFactory.isShutdown());
        Assert.assertTrue(threadFactory.getThreads().isEmpty());
        Assert.assertTrue(TestContextImpl.allContexts.isEmpty());
    }

}

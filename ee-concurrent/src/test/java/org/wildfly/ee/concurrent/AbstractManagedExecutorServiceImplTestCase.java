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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 *
 */
public abstract class AbstractManagedExecutorServiceImplTestCase {

    protected ManagedExecutorServiceImpl executor;

    protected ManagedExecutorServiceImpl newExecutor() {
        return TestExecutors.newManagedExecutorService();
    }

    protected void shutdownExecutor() throws Exception {
        executor.internalShutdown();
        executor.getTaskDecoratorExecutorService().shutdown();
        if (!executor.getTaskDecoratorExecutorService().awaitTermination(10, TimeUnit.SECONDS)) {
            fail();
        }
    }

    @Before
    public void beforeTest() {
        executor = newExecutor();
        Assert.assertTrue(TestContextImpl.allContexts.isEmpty());
    }

    @After
    public void afterTest() throws Exception {
        shutdownExecutor();
        Assert.assertTrue(TestContextImpl.allContexts.isEmpty());
    }

    protected void assertFutureTermination(Future future, boolean cancelled) {
        Assert.assertNotNull(future);
        Assert.assertTrue(future.isDone());
        Assert.assertEquals(cancelled, future.isCancelled());
    }

}

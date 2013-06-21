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

package org.wildfly.as.concurrent;

import org.wildfly.as.concurrent.context.ContextualManagedExecutorService;
import org.wildfly.as.concurrent.context.ManagedExecutorServiceAbortedTestCase;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 *
 */
public class ManagedExecutorServiceImplAbortedTestCase extends ManagedExecutorServiceAbortedTestCase {

    @Override
    protected ContextualManagedExecutorService newExecutor() {
        return TestExecutors.newComponentManagedExecutorService();
    }

    protected ManagedExecutorServiceImpl componentExecutor;

    @Override
    public void beforeTest() {
        super.beforeTest();
        componentExecutor = (ManagedExecutorServiceImpl) executor;
    }

    @Override
    protected void shutdownExecutor() throws Exception {
        componentExecutor.internalShutdown();
        componentExecutor.getTaskListenerExecutorService().shutdown();
        if (!componentExecutor.getTaskListenerExecutorService().awaitTermination(10, TimeUnit.SECONDS)) {
            fail();
        }
    }

}

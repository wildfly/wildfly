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

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Martins
 */
public class TestExecutors {

    private TestExecutors() {
    }

    public static ManagedExecutorServiceImpl newManagedExecutorService() {
        return newManagedExecutorService(newTaskDecoratorExecutorService());
    }

    public static ManagedExecutorServiceImpl newManagedExecutorService(TaskDecoratorExecutorService taskDecoratorExecutorService) {
        return new ManagedExecutorServiceImpl(taskDecoratorExecutorService, newContextConfiguration());
    }

    public static ManagedExecutorServiceImpl newManagedScheduledExecutorService() {
        return newManagedScheduledExecutorService(newTaskDecoratorScheduledExecutorService());
    }

    public static ManagedExecutorServiceImpl newManagedScheduledExecutorService(TaskDecoratorScheduledExecutorService taskDecoratorScheduledExecutorService) {
        return new ManagedScheduledExecutorServiceImpl(taskDecoratorScheduledExecutorService, newContextConfiguration());
    }

    public static ContextConfiguration newContextConfiguration() {
        return new TestContextConfiguration();
    }

    public static TaskDecoratorExecutorService newTaskDecoratorExecutorService() {
        return new TaskDecoratorThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }

    public static TaskDecoratorScheduledExecutorService newTaskDecoratorScheduledExecutorService() {
        return new TaskDecoratorScheduledThreadPoolExecutor(4);
    }
}

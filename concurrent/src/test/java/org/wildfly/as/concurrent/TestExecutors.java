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

import org.wildfly.as.concurrent.context.ContextConfiguration;
import org.wildfly.as.concurrent.context.TestContextExecutors;
import org.wildfly.as.concurrent.tasklistener.TaskListenerExecutorService;
import org.wildfly.as.concurrent.tasklistener.TaskListenerScheduledExecutorService;
import org.wildfly.as.concurrent.tasklistener.TestTaskListenerExecutors;

/**
 * @author Eduardo Martins
 */
public class TestExecutors {

    private TestExecutors() {
    }

    public static ManagedExecutorServiceImpl newComponentManagedExecutorService() {
        return newComponentManagedExecutorService(TestTaskListenerExecutors.newExecutor());
    }

    public static ManagedExecutorServiceImpl newComponentManagedExecutorService(TaskListenerExecutorService taskListenerExecutorService) {
        return new ManagedExecutorServiceImpl(taskListenerExecutorService, newContextConfiguration());
    }

    public static ManagedScheduledExecutorServiceImpl newComponentManagedScheduledExecutorService() {
        return newComponentManagedScheduledExecutorService(TestTaskListenerExecutors.newScheduledExecutor());
    }

    public static ManagedScheduledExecutorServiceImpl newComponentManagedScheduledExecutorService(TaskListenerScheduledExecutorService taskListenerScheduledExecutorService) {
        return new ManagedScheduledExecutorServiceImpl(taskListenerScheduledExecutorService, newContextConfiguration());
    }

    public static ContextConfiguration newContextConfiguration() {
        return TestContextExecutors.newContextConfiguration();
    }

}

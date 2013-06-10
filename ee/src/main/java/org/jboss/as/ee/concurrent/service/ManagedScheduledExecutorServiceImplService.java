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

package org.jboss.as.ee.concurrent.service;

import org.jboss.as.ee.EeMessages;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.ee.concurrent.ContextConfiguration;
import org.wildfly.ee.concurrent.ManagedScheduledExecutorServiceImpl;
import org.wildfly.ee.concurrent.TaskDecoratorScheduledExecutorService;

/**
 * Service responsible for creating, starting and stopping a ManagedScheduledExecutorServiceImpl.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceImplService implements Service<ManagedScheduledExecutorServiceImpl> {

    private ManagedScheduledExecutorServiceImpl executorService;

    private final InjectedValue<TaskDecoratorScheduledExecutorService> taskDecoratorScheduledExecutorService;
    private final ContextConfiguration contextConfiguration;

    public ManagedScheduledExecutorServiceImplService(ContextConfiguration contextConfiguration) {
        this.contextConfiguration = contextConfiguration;
        this.taskDecoratorScheduledExecutorService = new InjectedValue<>();
    }

    public void start(final StartContext context) throws StartException {
        executorService = new ManagedScheduledExecutorServiceImpl(taskDecoratorScheduledExecutorService.getValue(), contextConfiguration);
    }

    public void stop(final StopContext context) {
        if (executorService != null) {
            executorService.internalShutdown();
            this.executorService = null;
        }
    }

    public ManagedScheduledExecutorServiceImpl getValue() throws IllegalStateException {
        if (executorService == null) {
            throw EeMessages.MESSAGES.concurrentServiceValueUninitialized();
        }
        return executorService;
    }

    public Injector<TaskDecoratorScheduledExecutorService> getTaskDecoratorScheduledExecutorService() {
        return taskDecoratorScheduledExecutorService;
    }

}

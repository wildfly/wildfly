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
import org.jboss.as.ee.concurrent.TimeSpec;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.ee.concurrent.ManagedThreadFactoryImpl;
import org.wildfly.ee.concurrent.TaskDecoratorExecutorService;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service responsible for creating, starting and stopping the task listener scheduled thread pool executor, shared by every EE component default {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}.
 *
 * @author Eduardo Martins
 */
public abstract class AbstractTaskDecoratorExecutorServiceService<T extends TaskDecoratorExecutorService> implements Service<T> {

    private T executor;
    private StopContext context;
    private final ReentrantLock lock;

    final int maxThreads;
    final TimeSpec keepAlive;
    final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();

    public AbstractTaskDecoratorExecutorServiceService(final int maxThreads, final TimeSpec keepAlive) {
        this.maxThreads = maxThreads;
        this.keepAlive = keepAlive;
        this.lock = new ReentrantLock();
    }

    protected abstract T newExecutor();

    public void start(final StartContext context) throws StartException {
        lock.lock();
        try {
            if (threadFactoryValue.getOptionalValue() == null) {
                threadFactoryValue.setValue(new ImmediateValue<ThreadFactory>(new ManagedThreadFactoryImpl(null)));
            }
            executor = newExecutor();
        } finally {
            lock.unlock();
        }
    }

    public void stop(final StopContext context) {
        lock.lock();
        try {
            final T executor = getValue();
            this.context = context;
            context.asynchronous();
            executor.shutdown();
            this.executor = null;
        } finally {
            lock.unlock();
        }
    }

    void executorTerminated() {
        lock.lock();
        try {
            context.complete();
            context = null;
        } finally {
            lock.unlock();
        }
    }

    public T getValue() throws IllegalStateException {
        final T value = this.executor;
        if (value == null) {
            throw EeMessages.MESSAGES.concurrentServiceValueUninitialized();
        }
        return value;
    }

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

}

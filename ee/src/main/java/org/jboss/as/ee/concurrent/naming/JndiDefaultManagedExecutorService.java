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

package org.jboss.as.ee.concurrent.naming;

import org.jboss.as.ee.EeMessages;
import org.jboss.as.ee.concurrent.ConcurrentContext;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The jndi default ManagedExecutorService is a singleton bound to jndi, which just locates and delegates invocations to the real default ManagedExecutorService, obtained from the concurrent context set when invoked.
 *
 * @author Eduardo Martins
 */
public class JndiDefaultManagedExecutorService implements ManagedExecutorService {

    private static JndiDefaultManagedExecutorService ourInstance = new JndiDefaultManagedExecutorService();

    public static JndiDefaultManagedExecutorService getInstance() {
        return ourInstance;
    }

    private JndiDefaultManagedExecutorService() {
    }

    private ManagedExecutorService getCurrentDefaultManagedExecutorService() throws IllegalStateException {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext == null) {
            throw EeMessages.MESSAGES.noConcurrentContextCurrentlySet();
        }
        return concurrentContext.getDefaultManagedExecutorService();
    }

    @Override
    public void shutdown() {
        getCurrentDefaultManagedExecutorService().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return getCurrentDefaultManagedExecutorService().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getCurrentDefaultManagedExecutorService().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getCurrentDefaultManagedExecutorService().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getCurrentDefaultManagedExecutorService().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return getCurrentDefaultManagedExecutorService().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return getCurrentDefaultManagedExecutorService().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return getCurrentDefaultManagedExecutorService().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getCurrentDefaultManagedExecutorService().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return getCurrentDefaultManagedExecutorService().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getCurrentDefaultManagedExecutorService().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getCurrentDefaultManagedExecutorService().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        getCurrentDefaultManagedExecutorService().execute(command);
    }
}

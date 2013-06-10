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

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The jndi default ManagedScheduledExecutorService is a singleton bound to jndi, which just locates and delegates invocations to the real default ManagedScheduledExecutorService, obtained from the concurrent context set when invoked.
 *
 * @author Eduardo Martins
 */
public class JndiDefaultManagedScheduledExecutorService implements ManagedScheduledExecutorService {

    private static JndiDefaultManagedScheduledExecutorService ourInstance = new JndiDefaultManagedScheduledExecutorService();

    public static JndiDefaultManagedScheduledExecutorService getInstance() {
        return ourInstance;
    }

    private JndiDefaultManagedScheduledExecutorService() {
    }

    private ManagedScheduledExecutorService getCurrentManagedScheduledExecutorService() throws IllegalStateException {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext == null) {
            throw EeMessages.MESSAGES.noConcurrentContextCurrentlySet();
        }
        return concurrentContext.getDefaultManagedScheduledExecutorService();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return getCurrentManagedScheduledExecutorService().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
        return getCurrentManagedScheduledExecutorService().schedule(callable, trigger);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, Trigger trigger) {
        return getCurrentManagedScheduledExecutorService().schedule(command, trigger);
    }

    @Override
    public void shutdown() {
        getCurrentManagedScheduledExecutorService().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return getCurrentManagedScheduledExecutorService().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getCurrentManagedScheduledExecutorService().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getCurrentManagedScheduledExecutorService().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getCurrentManagedScheduledExecutorService().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return getCurrentManagedScheduledExecutorService().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return getCurrentManagedScheduledExecutorService().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return getCurrentManagedScheduledExecutorService().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getCurrentManagedScheduledExecutorService().invokeAll(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getCurrentManagedScheduledExecutorService().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getCurrentManagedScheduledExecutorService().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        getCurrentManagedScheduledExecutorService().execute(command);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return getCurrentManagedScheduledExecutorService().schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return getCurrentManagedScheduledExecutorService().schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return getCurrentManagedScheduledExecutorService().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return getCurrentManagedScheduledExecutorService().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}

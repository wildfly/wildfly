/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.concurrent;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.clustering.concurrent.ManagedExecutorService;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ManagedExecutorServiceTest {

    private final ExecutorService executor;
    private final ExecutorService subject;

    public ManagedExecutorServiceTest() {
        this(mock(ExecutorService.class));
    }

    private ManagedExecutorServiceTest(ExecutorService executor) {
        this(executor, new ManagedExecutorService(executor));
    }

    protected ManagedExecutorServiceTest(ExecutorService executor, ExecutorService subject) {
        this.executor = executor;
        this.subject = subject;
    }

    @Test
    public void submitCallable() {
        @SuppressWarnings("unchecked")
        Future<Object> expected = mock(Future.class);
        Task task = new Task();

        when(this.executor.submit(task)).thenReturn(expected);

        Future<Object> result = this.subject.submit(task);

        assertSame(expected, result);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void submitRunnable() {
        Future expected = mock(Future.class);
        Runnable task = mock(Runnable.class);

        when(this.executor.submit(task)).thenReturn(expected);

        Future<?> result = this.subject.submit(task);

        assertSame(expected, result);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void submitRunnableWithResult() {
        Future expected = mock(Future.class);
        Runnable task = mock(Runnable.class);
        Object r = new Object();

        when(this.executor.submit(task, r)).thenReturn(expected);

        Future<?> result = this.subject.submit(task, r);

        assertSame(expected, result);
    }

    @Test
    public void execute() {
        Runnable task = mock(Runnable.class);

        this.subject.execute(task);

        verify(this.executor).execute(task);
    }

    @Test
    public void invokeAll() throws InterruptedException {
        Collection<Task> tasks = Collections.singletonList(new Task());
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        List<Future<Object>> expected = Collections.singletonList(future);

        when(this.executor.invokeAll(tasks)).thenReturn(expected);

        List<Future<Object>> result = this.subject.invokeAll(tasks);

        assertSame(expected, result);
    }

    @Test
    public void invokeAllWithTimeout() throws InterruptedException {
        Collection<Task> tasks = Collections.singletonList(new Task());
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        List<Future<Object>> expected = Collections.singletonList(future);
        long timeout = 10L;
        TimeUnit unit = TimeUnit.SECONDS;

        when(this.executor.invokeAll(tasks, timeout, unit)).thenReturn(expected);

        List<Future<Object>> result = this.subject.invokeAll(tasks, timeout, unit);

        assertSame(expected, result);
    }

    @Test
    public void invokeAny() throws InterruptedException, ExecutionException {
        Collection<Task> tasks = Collections.singletonList(new Task());
        Object expected = new Object();

        when(this.executor.invokeAny(tasks)).thenReturn(expected);

        Object result = this.subject.invokeAny(tasks);

        assertSame(expected, result);
    }

    @Test
    public void invokeAnyWithTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        Collection<Task> tasks = Collections.singletonList(new Task());
        Object expected = new Object();
        long timeout = 10L;
        TimeUnit unit = TimeUnit.SECONDS;

        when(this.executor.invokeAny(tasks, timeout, unit)).thenReturn(expected);

        Object result = this.subject.invokeAny(tasks, timeout, unit);

        assertSame(expected, result);
    }

    @Test
    public void shutdown() {
        this.subject.shutdown();

        verifyZeroInteractions(this.executor);
    }

    @Test
    public void shutdownNow() {
        List<Runnable> result = this.subject.shutdownNow();

        verifyZeroInteractions(this.executor);

        assertTrue(result.isEmpty());
    }

    @Test
    public void isShutdown() {
        when(this.executor.isShutdown()).thenReturn(true);

        boolean result = this.subject.isShutdown();

        assertTrue(result);
    }

    @Test
    public void isTerminated() {
        when(this.executor.isTerminated()).thenReturn(true);

        boolean result = this.subject.isTerminated();

        assertTrue(result);
    }

    @Test
    public void awaitTermination() throws InterruptedException {
        when(this.executor.isTerminated()).thenReturn(true);

        boolean result = this.subject.awaitTermination(1L, TimeUnit.SECONDS);

        assertTrue(result);
    }

    static class Task implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            return null;
        }
    }
}

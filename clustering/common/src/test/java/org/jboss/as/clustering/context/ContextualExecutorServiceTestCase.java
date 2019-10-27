/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.context;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Paul Ferraro
 */
public class ContextualExecutorServiceTestCase {

    private final ExecutorService executor = mock(ExecutorService.class);
    private final Contextualizer contextualizer = mock(Contextualizer.class);
    private final ExecutorService subject = new ContextualExecutorService(this.executor, this.contextualizer);

    @After
    public void after() {
        reset(this.executor, this.contextualizer);
    }

    @Test
    public void execute() {
        Runnable command = mock(Runnable.class);
        Runnable contextualCommand = mock(Runnable.class);

        when(this.contextualizer.contextualize(same(command))).thenReturn(contextualCommand);

        this.subject.execute(command);

        verify(this.executor).execute(contextualCommand);
    }

    @Test
    public void shutdown() {
        this.subject.shutdown();

        verify(this.executor).shutdown();
    }

    @Test
    public void shutdownNow() {
        List<Runnable> expected = Collections.singletonList(mock(Runnable.class));

        when(this.executor.shutdownNow()).thenReturn(expected);

        List<Runnable> result = this.subject.shutdownNow();

        assertSame(expected, result);
    }

    @Test
    public void isShutdown() {
        when(this.executor.isShutdown()).thenReturn(false, true);

        assertFalse(this.subject.isShutdown());
        assertTrue(this.subject.isShutdown());
    }

    @Test
    public void isTerminated() {
        when(this.executor.isTerminated()).thenReturn(false, true);

        assertFalse(this.subject.isTerminated());
        assertTrue(this.subject.isTerminated());
    }

    @Test
    public void awaitTermination() throws InterruptedException {
        when(this.executor.awaitTermination(10L, TimeUnit.MINUTES)).thenReturn(false, true);

        assertFalse(this.subject.awaitTermination(10L, TimeUnit.MINUTES));
        assertTrue(this.subject.awaitTermination(10L, TimeUnit.MINUTES));
    }

    @Test
    public void submitCallable() {
        Callable<Object> task = mock(Callable.class);
        Callable<Object> contextualTask = mock(Callable.class);
        Future<Object> expected = mock(Future.class);

        when(this.contextualizer.contextualize(task)).thenReturn(contextualTask);
        when(this.executor.submit(same(contextualTask))).thenReturn(expected);

        Future<Object> result = this.subject.submit(task);

        assertSame(expected, result);
    }

    @Test
    public void submitRunnableWithResult() {
        Runnable task = mock(Runnable.class);
        Runnable contextualTask = mock(Runnable.class);
        Future<Object> expected = mock(Future.class);
        Object param = new Object();

        when(this.contextualizer.contextualize(task)).thenReturn(contextualTask);
        when(this.executor.submit(same(contextualTask), same(param))).thenReturn(expected);

        Future<Object> result = this.subject.submit(task, param);

        assertSame(expected, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void submit() {
        Runnable task = mock(Runnable.class);
        Runnable contextualTask = mock(Runnable.class);
        @SuppressWarnings("rawtypes")
        Future expected = mock(Future.class);

        when(this.contextualizer.contextualize(task)).thenReturn(contextualTask);
        when(this.executor.submit(same(contextualTask))).thenReturn(expected);

        Future<?> result = this.subject.submit(task);

        assertSame(expected, result);
    }

    @Test
    public void invokeAll() throws InterruptedException {
        Callable<Object> task1 = mock(Callable.class);
        Callable<Object> task2 = mock(Callable.class);
        Callable<Object> task3 = mock(Callable.class);
        Callable<Object> contextualTask1 = mock(Callable.class);
        Callable<Object> contextualTask2 = mock(Callable.class);
        Callable<Object> contextualTask3 = mock(Callable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Callable<Object>>> capturedTasks = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        List<Future<Object>> expected = Collections.singletonList(mock(Future.class));

        when(this.contextualizer.contextualize(task1)).thenReturn(contextualTask1);
        when(this.contextualizer.contextualize(task2)).thenReturn(contextualTask2);
        when(this.contextualizer.contextualize(task3)).thenReturn(contextualTask3);
        when(this.executor.invokeAll(capturedTasks.capture())).thenReturn(expected);

        List<Future<Object>> result = this.subject.invokeAll(Arrays.asList(task1, task2, task3));

        assertSame(expected, result);

        List<Callable<Object>> tasks = capturedTasks.getValue();
        assertEquals(3, tasks.size());
        assertSame(contextualTask1, tasks.get(0));
        assertSame(contextualTask2, tasks.get(1));
        assertSame(contextualTask3, tasks.get(2));
    }

    @Test
    public void invokeAllWithTimeout() throws InterruptedException {
        Callable<Object> task1 = mock(Callable.class);
        Callable<Object> task2 = mock(Callable.class);
        Callable<Object> task3 = mock(Callable.class);
        Callable<Object> contextualTask1 = mock(Callable.class);
        Callable<Object> contextualTask2 = mock(Callable.class);
        Callable<Object> contextualTask3 = mock(Callable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Callable<Object>>> capturedTasks = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        List<Future<Object>> expected = Collections.singletonList(mock(Future.class));

        when(this.contextualizer.contextualize(task1)).thenReturn(contextualTask1);
        when(this.contextualizer.contextualize(task2)).thenReturn(contextualTask2);
        when(this.contextualizer.contextualize(task3)).thenReturn(contextualTask3);
        when(this.executor.invokeAll(capturedTasks.capture(), eq(10L), same(TimeUnit.MINUTES))).thenReturn(expected);

        List<Future<Object>> result = this.subject.invokeAll(Arrays.asList(task1, task2, task3), 10L, TimeUnit.MINUTES);

        assertSame(expected, result);

        List<Callable<Object>> tasks = capturedTasks.getValue();
        assertEquals(3, tasks.size());
        assertSame(contextualTask1, tasks.get(0));
        assertSame(contextualTask2, tasks.get(1));
        assertSame(contextualTask3, tasks.get(2));
    }

    @Test
    public void invokeAny() throws InterruptedException, ExecutionException {
        Callable<Object> task1 = mock(Callable.class);
        Callable<Object> task2 = mock(Callable.class);
        Callable<Object> task3 = mock(Callable.class);
        Callable<Object> contextualTask1 = mock(Callable.class);
        Callable<Object> contextualTask2 = mock(Callable.class);
        Callable<Object> contextualTask3 = mock(Callable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Callable<Object>>> capturedTasks = ArgumentCaptor.forClass(List.class);
        Object expected = new Object();

        when(this.contextualizer.contextualize(task1)).thenReturn(contextualTask1);
        when(this.contextualizer.contextualize(task2)).thenReturn(contextualTask2);
        when(this.contextualizer.contextualize(task3)).thenReturn(contextualTask3);
        when(this.executor.invokeAny(capturedTasks.capture())).thenReturn(expected);

        Object result = this.subject.invokeAny(Arrays.asList(task1, task2, task3));

        assertSame(expected, result);

        List<Callable<Object>> tasks = capturedTasks.getValue();
        assertEquals(3, tasks.size());
        assertSame(contextualTask1, tasks.get(0));
        assertSame(contextualTask2, tasks.get(1));
        assertSame(contextualTask3, tasks.get(2));
    }

    @Test
    public void invokeAnyWithTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        Callable<Object> task1 = mock(Callable.class);
        Callable<Object> task2 = mock(Callable.class);
        Callable<Object> task3 = mock(Callable.class);
        Callable<Object> contextualTask1 = mock(Callable.class);
        Callable<Object> contextualTask2 = mock(Callable.class);
        Callable<Object> contextualTask3 = mock(Callable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Callable<Object>>> capturedTasks = ArgumentCaptor.forClass(List.class);
        Object expected = new Object();

        when(this.contextualizer.contextualize(task1)).thenReturn(contextualTask1);
        when(this.contextualizer.contextualize(task2)).thenReturn(contextualTask2);
        when(this.contextualizer.contextualize(task3)).thenReturn(contextualTask3);
        when(this.executor.invokeAny(capturedTasks.capture(), eq(10L), same(TimeUnit.MINUTES))).thenReturn(expected);

        Object result = this.subject.invokeAny(Arrays.asList(task1, task2, task3), 10L, TimeUnit.MINUTES);

        assertSame(expected, result);

        List<Callable<Object>> tasks = capturedTasks.getValue();
        assertEquals(3, tasks.size());
        assertSame(contextualTask1, tasks.get(0));
        assertSame(contextualTask2, tasks.get(1));
        assertSame(contextualTask3, tasks.get(2));
    }
}

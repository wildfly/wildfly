/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class StampedLockServiceExecutorTestCase {

    @Test
    public void testExecuteRunnable() {
        ServiceExecutor executor = new StampedLockServiceExecutor();

        Runnable executeTask = mock(Runnable.class);

        executor.execute(executeTask);

        // Task should run
        verify(executeTask).run();
        reset(executeTask);

        Runnable closeTask = mock(Runnable.class);

        executor.close(closeTask);

        verify(closeTask).run();
        reset(closeTask);

        executor.close(closeTask);

        // Close task should only run once
        verify(closeTask, never()).run();

        executor.execute(executeTask);

        // Task should no longer run since service is closed
        verify(executeTask, never()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteCallable() throws Exception {
        ServiceExecutor executor = new StampedLockServiceExecutor();
        Object expected = new Object();

        Callable<Object> executeTask = mock(Callable.class);

        when(executeTask.call()).thenReturn(expected);

        Optional<Object> result = executor.execute(executeTask);

        // Task should run
        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        reset(executeTask);

        Runnable closeTask = mock(Runnable.class);

        executor.close(closeTask);

        verify(closeTask).run();
        reset(closeTask);

        executor.close(closeTask);

        // Close task should only run once
        verify(closeTask, never()).run();

        result = executor.execute(executeTask);

        // Task should no longer run since service is closed
        assertFalse(result.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteCallableException() throws Exception {
        ServiceExecutor executor = new StampedLockServiceExecutor();
        Exception expected = new Exception();

        Callable<Object> executeTask = mock(Callable.class);

        when(executeTask.call()).thenThrow(expected);

        try {
            executor.execute(executeTask);
            fail("Execute should have thrown expected exception");
        } catch (Exception e) {
            // Task should run
            assertSame(expected, e);
        }
        reset(executeTask);

        Runnable closeTask = mock(Runnable.class);

        executor.close(closeTask);

        verify(closeTask).run();
        reset(closeTask);

        executor.close(closeTask);

        // Close task should only run once
        verify(closeTask, never()).run();

        try {
            Optional<Object> result = executor.execute(executeTask);

            // Task should no longer run since service is closed
            assertFalse(result.isPresent());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteSupplier() {
        ServiceExecutor executor = new StampedLockServiceExecutor();
        Object expected = new Object();

        Supplier<Object> executeTask = mock(Supplier.class);

        when(executeTask.get()).thenReturn(expected);

        Optional<Object> result = executor.execute(executeTask);

        // Task should run
        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        reset(executeTask);

        Runnable closeTask = mock(Runnable.class);

        executor.close(closeTask);

        verify(closeTask).run();
        reset(closeTask);

        executor.close(closeTask);

        // Close task should only run once
        verify(closeTask, never()).run();

        result = executor.execute(executeTask);

        // Task should no longer run since service is closed
        assertFalse(result.isPresent());
    }

    @Test
    public void concurrent() throws InterruptedException, ExecutionException {
        ServiceExecutor executor = new StampedLockServiceExecutor();

        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch executeLatch = new CountDownLatch(1);
            CountDownLatch stopLatch = new CountDownLatch(1);
            Runnable executeTask = () -> {
                try {
                    executeLatch.countDown();
                    stopLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            Future<?> executeFuture = service.submit(() -> executor.execute(executeTask));

            executeLatch.await();

            Runnable closeTask = mock(Runnable.class);

            Future<?> closeFuture = service.submit(() -> executor.close(closeTask));

            Thread.yield();

            // Verify that stop is blocked
            verify(closeTask, never()).run();

            stopLatch.countDown();

            executeFuture.get();
            closeFuture.get();

            // Verify close task was invoked, now that execute task is complete
            verify(closeTask).run();
        } finally {
            service.shutdownNow();
        }
    }
}

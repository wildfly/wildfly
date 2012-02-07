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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.concurrent.ManagedScheduledExecutorService;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ManagedScheduledExecutorTest extends ManagedExecutorServiceTest {

    private final ScheduledExecutorService executor;
    private final ScheduledExecutorService subject;

    public ManagedScheduledExecutorTest() {
        this(mock(ScheduledExecutorService.class));
    }

    private ManagedScheduledExecutorTest(ScheduledExecutorService executor) {
        this(executor, new ManagedScheduledExecutorService(executor));
    }

    private ManagedScheduledExecutorTest(ScheduledExecutorService executor, ScheduledExecutorService subject) {
        super(executor, subject);
        this.executor = executor;
        this.subject = subject;
    }

    @Override
    public void isShutdown() {
        boolean result = this.subject.isShutdown();

        verifyZeroInteractions(this.executor);

        assertFalse(result);
    }

    @Override
    public void isTerminated() {
        boolean result = this.subject.isTerminated();

        verifyZeroInteractions(this.executor);

        assertFalse(result);
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        boolean result = this.subject.awaitTermination(1L, TimeUnit.SECONDS);

        verifyZeroInteractions(this.executor);

        assertFalse(result);
    }

    @Test
    public void scheduleCallable() {
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> expected = mock(ScheduledFuture.class);
        Task task = new Task();
        long delay = 10L;
        TimeUnit unit = TimeUnit.SECONDS;

        when(this.executor.schedule(task, delay, unit)).thenReturn(expected);

        ScheduledFuture<Object> result = this.subject.schedule(task, delay, unit);

        assertSame(expected, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void scheduleAtFixedRate() {
        @SuppressWarnings("rawtypes")
        ScheduledFuture expected = mock(ScheduledFuture.class);
        Runnable task = mock(Runnable.class);
        long delay = 10L;
        long period = 20L;
        TimeUnit unit = TimeUnit.SECONDS;

        when(this.executor.scheduleAtFixedRate(task, delay, period, unit)).thenReturn(expected);

        ScheduledFuture<?> result = this.subject.scheduleAtFixedRate(task, delay, period, unit);

        assertSame(expected, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void scheduleWithFixedDelay() {
        @SuppressWarnings("rawtypes")
        ScheduledFuture expected = mock(ScheduledFuture.class);
        Runnable task = mock(Runnable.class);
        long delay = 10L;
        long period = 20L;
        TimeUnit unit = TimeUnit.SECONDS;

        when(this.executor.scheduleWithFixedDelay(task, delay, period, unit)).thenReturn(expected);

        ScheduledFuture<?> result = this.subject.scheduleWithFixedDelay(task, delay, period, unit);

        assertSame(expected, result);
    }
}

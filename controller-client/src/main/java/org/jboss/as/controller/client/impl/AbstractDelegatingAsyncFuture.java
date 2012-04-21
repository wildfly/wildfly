/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.impl;

import org.jboss.threads.AsyncFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractDelegatingAsyncFuture<T> implements AsyncFuture<T> {

    private final AsyncFuture<T> delegate;
    public AbstractDelegatingAsyncFuture(AsyncFuture<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Status await() throws InterruptedException {
        return delegate.await();
    }

    @Override
    public Status await(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.await(timeout, unit);
    }

    @Override
    public T getUninterruptibly() throws CancellationException, ExecutionException {
        return delegate.getUninterruptibly();
    }

    @Override
    public T getUninterruptibly(long timeout, TimeUnit unit) throws CancellationException, ExecutionException, TimeoutException {
        return delegate.getUninterruptibly(timeout, unit);
    }

    @Override
    public Status awaitUninterruptibly() {
        return delegate.awaitUninterruptibly();
    }

    @Override
    public Status awaitUninterruptibly(long timeout, TimeUnit unit) {
        return delegate.awaitUninterruptibly(timeout, unit);
    }

    @Override
    public Status getStatus() {
        return delegate.getStatus();
    }

    public <A> void addListener(Listener<? super T, A> aListener, A attachment) {
        delegate.addListener(aListener, attachment);
    }

    @Override
    public boolean cancel(boolean interruptionDesired) {
        // allow custom cancellation policies
        asyncCancel(interruptionDesired);
        return awaitUninterruptibly() == Status.CANCELLED;
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.TimeUnit;

import org.jboss.threads.AsyncFuture;

/**
 * Base class for {@link org.jboss.as.controller.client.impl.AbstractDelegatingAsyncFuture}
 * and {@link org.jboss.as.controller.client.impl.ConvertingDelegatingAsyncFuture} that handles the
 * simple delegation stuff.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
abstract class BasicDelegatingAsyncFuture<T, D> implements AsyncFuture<T> {

    final AsyncFuture<D> delegate;

    BasicDelegatingAsyncFuture(AsyncFuture<D> delegate) {
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
}

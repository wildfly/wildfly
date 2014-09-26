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

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.ControllerClientLogger;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;

/**
 * Converts from {@code AsyncFuture<OperationResponse>} to {@code AsyncFuture<ModelNode>}.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
class ConvertingDelegatingAsyncFuture extends BasicDelegatingAsyncFuture<ModelNode, OperationResponse> {

    ConvertingDelegatingAsyncFuture(AsyncFuture<OperationResponse> delegate) {
        super(delegate);
    }

    @Override
    public ModelNode getUninterruptibly() throws CancellationException, ExecutionException {
        return responseNodeOnly(delegate.getUninterruptibly());
    }

    @Override
    public ModelNode getUninterruptibly(long timeout, TimeUnit unit) throws CancellationException, ExecutionException, TimeoutException {
        return responseNodeOnly(delegate.getUninterruptibly(timeout, unit));
    }

    @Override
    public <A> void addListener(final Listener<? super ModelNode, A> listener, A attachment) {
        delegate.addListener(convertListener(this, listener), attachment);
    }

    @Override
    public ModelNode get() throws InterruptedException, ExecutionException {
        return responseNodeOnly(delegate.get());
    }

    @Override
    public ModelNode get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return responseNodeOnly(delegate.get(timeout, unit));
    }

    @Override
    public void asyncCancel(boolean interruptionDesired) {
        delegate.asyncCancel(interruptionDesired);
    }

    /** Extracts the response node from an OperationResponse and returns it after first closing the OperationResponse */
    private static ModelNode responseNodeOnly(OperationResponse or) {
        ModelNode result = or.getResponseNode();
        try {
            or.close();
        } catch (IOException e) {
            ControllerClientLogger.ROOT_LOGGER.debugf(e, "Caught exception closing %s whose associated streams, " +
                    "if any, were not wanted", or);
        }
        return result;
    }

    private static <A> Listener<? super OperationResponse, A> convertListener(final AsyncFuture<ModelNode> caller,
                                                                      final Listener<? super ModelNode, A> listener) {
        return new Listener<OperationResponse, A>() {
            @Override
            public void handleComplete(AsyncFuture<? extends OperationResponse> future, A attachment) {
                listener.handleComplete(caller, attachment);
            }

            @Override
            public void handleFailed(AsyncFuture<? extends OperationResponse> future, Throwable cause, A attachment) {
                listener.handleFailed(caller, cause, attachment);
            }

            @Override
            public void handleCancelled(AsyncFuture<? extends OperationResponse> future, A attachment) {
                listener.handleCancelled(caller, attachment);
            }
        };
    }
}

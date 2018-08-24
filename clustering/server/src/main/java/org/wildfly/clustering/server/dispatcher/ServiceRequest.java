/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.dispatcher;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jgroups.Address;
import org.jgroups.SuspectedException;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.UnicastRequest;
import org.jgroups.util.Buffer;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;

/**
 * Translates a {@link NoSuchService} response to a {@link CancellationException}.
 * @author Paul Ferraro
 */
public class ServiceRequest<T> extends UnicastRequest<T> {

    public ServiceRequest(RequestCorrelator correlator, Address target, RequestOptions options) {
        super(correlator, target, options);
    }

    public CompletionStage<T> send(Buffer data) throws CommandDispatcherException {
        try {
            this.sendRequest(data);
            return this;
        } catch (Exception e) {
            throw new CommandDispatcherException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receiveResponse(Object value, Address sender, boolean exceptional) {
        if (this.isDone()) return;

        if (exceptional) {
            this.completeExceptionally((Throwable) value);
        } else if (value instanceof NoSuchService) {
            this.completeExceptionally(new CancellationException());
        } else {
            this.complete((T) value);
        }
        this.corrDone();
    }

    @Override
    public boolean completeExceptionally(Throwable exception) {
        return super.completeExceptionally((exception instanceof SuspectedException) ? new CancellationException() : exception);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            // Wait at most for the configured timeout
            // If the message was dropped by the receiver, this would otherwise block forever
            return super.get(super.options.timeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // Auto-cancel on timeout
            this.cancel(true);
            throw new CancellationException(e.getLocalizedMessage());
        }
    }

    @Override
    public T join() {
        try {
            return this.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}

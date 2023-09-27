/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.dispatcher;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.UnicastRequest;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * Translates a {@link NoSuchService} response to a {@link CancellationException}.
 * @author Paul Ferraro
 */
public class ServiceRequest<T, C> extends UnicastRequest<T> {

    private final C context;

    public ServiceRequest(RequestCorrelator correlator, Address target, RequestOptions options, C context) {
        super(correlator, target, options);
        this.context = context;
    }

    public CompletionStage<T> send(Message message) throws CommandDispatcherException {
        try {
            this.sendRequest(message);
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
            MarshalledValue<T, C> marshalledValue = (MarshalledValue<T, C>) value;
            try {
                this.complete(marshalledValue.get(this.context));
            } catch (IOException e) {
                this.completeExceptionally(e);
            }
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

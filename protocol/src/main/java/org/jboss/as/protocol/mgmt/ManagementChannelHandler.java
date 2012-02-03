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

package org.jboss.as.protocol.mgmt;

import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.remoting3.Channel;
import org.jboss.threads.AsyncFuture;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Generic management channel handler allowing to assemble multiple {@code ManagementRequestHandlerFactory} per channel.
 *
 * @author Emanuel Muckenhuber
 */
public final class ManagementChannelHandler extends AbstractMessageHandler implements ManagementChannelAssociation {

    private static final AtomicReferenceFieldUpdater<ManagementChannelHandler, ManagementRequestHandlerFactory[]> updater = AtomicReferenceFieldUpdater.newUpdater(ManagementChannelHandler.class, ManagementRequestHandlerFactory[].class, "handlers");
    private static final ManagementRequestHandlerFactory[] NO_HANDLERS = new ManagementRequestHandlerFactory[0];

    /** The management request handlers. */
    @SuppressWarnings("ALL")
    private volatile ManagementRequestHandlerFactory[] handlers;

    // A receiver for this handler
    private final Channel.Receiver receiver;
    // The management client strategy
    private final ManagementClientChannelStrategy strategy;

    public ManagementChannelHandler(final Channel channel, final ExecutorService executorService) {
        this(ManagementClientChannelStrategy.create(channel), executorService);
    }

    public ManagementChannelHandler(final Channel channel, final ExecutorService executorService, final ManagementRequestHandlerFactory... initial) {
        this(ManagementClientChannelStrategy.create(channel), executorService, initial);
    }

    public ManagementChannelHandler(final ManagementClientChannelStrategy strategy, final ExecutorService executorService) {
        this(strategy, executorService, NO_HANDLERS);
    }

    public ManagementChannelHandler(final ManagementClientChannelStrategy strategy, final ExecutorService executorService, final ManagementRequestHandlerFactory... initial) {
        super(executorService);
        this.strategy = strategy;
        this.handlers = initial;
        this.receiver = ManagementChannelReceiver.createDelegating(this);
    }

    /** {@inheritDoc} */
    @Override
    public Channel getChannel() throws IOException {
        return strategy.getChannel();
    }

    /** {@inheritDoc} */
    @Override
    public <T, A> ActiveOperation<T, A> executeRequest(ManagementRequest<T, A> request, A attachment, ActiveOperation.CompletedCallback<T> callback) throws IOException {
        final ActiveOperation<T, A> operation = super.registerActiveOperation(attachment, callback);
        executeRequest(operation, request);
        return operation;
    }

    /** {@inheritDoc} */
    @Override
    public <T, A> ActiveOperation<T, A> executeRequest(final ManagementRequest<T, A> request, final A attachment) throws IOException {
        final ActiveOperation<T, A> operation = super.registerActiveOperation(attachment);
        executeRequest(operation, request);
        return operation;
    }

    /** {@inheritDoc} */
    @Override
    public <T, A> AsyncFuture<T> executeRequest(final Integer operationId, final ManagementRequest<T, A> request) throws IOException {
        final ActiveOperation<T, A> operation = super.getActiveOperation(operationId);
        if(operation == null) {
            throw ProtocolMessages.MESSAGES.responseHandlerNotFound(operationId);
        }
        return executeRequest(operation, request);
    }

    /** {@inheritDoc} */
    @Override
    public <T, A> AsyncFuture<T> executeRequest(final ActiveOperation<T, A> support, final ManagementRequest<T, A> request) throws IOException {
        return super.executeRequest(request, strategy.getChannel(), support);
    }

    /** {@inheritDoc} */
    @Override
    protected ManagementRequestHandler<?, ?> getRequestHandler(final ManagementRequestHeader header) {
        final ManagementRequestHandlerFactory[] snapshot = updater.get(this);
        // Iterate through the registered handlers
        return new ManagementRequestHandlerFactory.RequestHandlerChain() {
            final int length = snapshot.length;
            private int index = -1;

            @Override
            public ManagementRequestHandler<?, ?> resolveNext() {
                if(index++ == length) {
                    return getFallbackHandler(header);
                }
                final ManagementRequestHandlerFactory factory = snapshot[index];
                if(factory == null) {
                    // return getFallbackHandler(header);
                    return resolveNext();
                }
                return factory.resolveHandler(this, header);
            }

            @Override
            public <T, A> ActiveOperation<T, A> createActiveOperation(A attachment) {
                return ManagementChannelHandler.this.registerActiveOperation(attachment);
            }

            @Override
            public <T, A> ActiveOperation<T, A> createActiveOperation(A attachment, ActiveOperation.CompletedCallback<T> completedCallback) {
                return ManagementChannelHandler.this.registerActiveOperation(attachment, completedCallback);
            }

            @Override
            public <T, A> ActiveOperation<T, A> registerActiveOperation(Integer id, A attachment) {
                return ManagementChannelHandler.this.registerActiveOperation(id, attachment);
            }

            @Override
            public <T, A> ActiveOperation<T, A> registerActiveOperation(Integer id, A attachment, ActiveOperation.CompletedCallback<T> completedCallback) {
                return ManagementChannelHandler.this.registerActiveOperation(id, attachment, completedCallback);
            }

        }.resolveNext();
    }

    /**
     * Get a receiver instance for this context.
     *
     * @return the receiver
     */
    public Channel.Receiver getReceiver() {
        return receiver;
    }

    /**
     * Add a management request handler factory to this context.
     *
     * @param factory the request handler to add
     */
    public void addHandlerFactory(ManagementRequestHandlerFactory factory) {
        for (;;) {
            final ManagementRequestHandlerFactory[] snapshot = updater.get(this);
            final int length = snapshot.length;
            final ManagementRequestHandlerFactory[] newVal = new ManagementRequestHandlerFactory[length + 1];
            System.arraycopy(snapshot, 0, newVal, 0, length);
            newVal[length] = factory;
            if (updater.compareAndSet(this, snapshot, newVal)) {
                return;
            }
        }
    }

    /**
     * Remove a management request handler factory from this context.
     *
     * @param instance the request handler factory
     * @return {@code true} if the instance was removed, {@code false} otherwise
     */
    public boolean removeHandlerFactory(ManagementRequestHandlerFactory instance) {
        for(;;) {
            final ManagementRequestHandlerFactory[] snapshot = updater.get(this);
            final int length = snapshot.length;
            int index = -1;
            for(int i = 0; i < length; i++) {
                if(snapshot[i] == instance) {
                    index = i;
                    break;
                }
            }
            if(index == -1) {
                return false;
            }
            final ManagementRequestHandlerFactory[] newVal = new ManagementRequestHandlerFactory[length - 1];
            System.arraycopy(snapshot, 0, newVal, 0, index);
            System.arraycopy(snapshot, index + 1, newVal, index, length - index - 1);
            if (updater.compareAndSet(this, snapshot, newVal)) {
                return true;
            }
        }
    }

}

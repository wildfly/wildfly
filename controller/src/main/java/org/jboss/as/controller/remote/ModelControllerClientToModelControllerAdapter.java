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
package org.jboss.as.controller.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.CancellationException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.Connection;
import org.jboss.dmr.ModelNode;

/**
 * Adapter from the remote model controller client interfaces to the main model controller interfaces
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class ModelControllerClientToModelControllerAdapter implements ModelController {

    final ModelControllerClient client;

    /**
     * Create a new model controller adapter connecting to a remote host
     *
     * @param type the type of controller to connect to
     * @param address the address of the remote model controller to connect to
     * @param port the port of the remote model controller to connect to
     */
    ModelControllerClientToModelControllerAdapter(final ModelControllerClient.Type type, final InetAddress address, final int port) {
        client = ModelControllerClient.Factory.create(type, address, port);
    }

    /**
     * Create a new model controller adapter reusing an existing connection
     *
     * @param connection the connection
     */
    public ModelControllerClientToModelControllerAdapter(final ModelControllerClient.Type type, final Connection connection) {
        client = ModelControllerClient.Factory.create(type, connection);
    }

    @Override
    public OperationResult execute(final ModelNode operation, final ResultHandler handler) {
        return new OperationHandlerResultAdapter(client.execute(operation, new ResultHandlerAdapter(handler)));
    }

    @Override
    public ModelNode execute(final ModelNode operation) throws CancellationException {
        try {
            return client.execute(operation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class OperationHandlerResultAdapter implements OperationResult, Cancellable {
        private final org.jboss.as.controller.client.OperationResult delegate;

        OperationHandlerResultAdapter(org.jboss.as.controller.client.OperationResult delegate){
            this.delegate = delegate;
        }

        @Override
        public Cancellable getCancellable() {
            return this;
        }

        @Override
        public ModelNode getCompensatingOperation() {
            return delegate.getCompensatingOperation();
        }

        @Override
        public boolean cancel() {
            try {
                return delegate.getCancellable().cancel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ResultHandlerAdapter implements org.jboss.as.controller.client.ResultHandler {
        private final ResultHandler delegate;

        public ResultHandlerAdapter(final ResultHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handleResultFragment(final String[] location, final ModelNode result) {
            delegate.handleResultFragment(location, result);
        }

        @Override
        public void handleResultComplete() {
            delegate.handleResultComplete();
        }

        @Override
        public void handleCancellation() {
            delegate.handleCancellation();
        }

        @Override
        public void handleException(final Exception e) {
            //TODO
        }
    }
}

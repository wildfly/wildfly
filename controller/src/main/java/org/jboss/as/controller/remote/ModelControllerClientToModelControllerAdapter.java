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
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ModelControllerClientToModelControllerAdapter implements ModelController {

    final ModelControllerClient client;

    public ModelControllerClientToModelControllerAdapter(final InetAddress address, final int port) {
        client = ModelControllerClient.Factory.create(address, port);
    }

    @Override
    public Cancellable execute(final ModelNode operation, final ResultHandler handler) {
        return new CancellableAdapter(client.execute(operation, new ResultHandlerAdapter(handler)));
    }

    @Override
    public ModelNode execute(final ModelNode operation) throws CancellationException, OperationFailedException {
        try {
            return client.execute(operation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class CancellableAdapter implements Cancellable {
        private final org.jboss.as.controller.client.Cancellable delegate;

        CancellableAdapter(org.jboss.as.controller.client.Cancellable delegate){
            this.delegate = delegate;
        }

        @Override
        public void cancel() {
            try {
                delegate.cancel();
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
        public void handleResultComplete(final ModelNode compensatingOperation) {
            delegate.handleResultComplete(compensatingOperation);
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

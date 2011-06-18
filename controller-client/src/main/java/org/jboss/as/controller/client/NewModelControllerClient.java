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

package org.jboss.as.controller.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;

/**
 * A client for an application server management model controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface NewModelControllerClient extends Closeable {

    /**
     * Execute an operation synchronously.
     *
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode execute(ModelNode operation) throws IOException;
    /**
     * Execute an operation synchronously.
     *
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode execute(NewOperation operation) throws IOException;

    /**
     * Execute an operation synchronously, optionally receiving progress reports.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode execute(ModelNode operation, OperationMessageHandler messageHandler) throws IOException;

    /**
     * Execute an operation synchronously, optionally receiving progress reports.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode execute(NewOperation operation, OperationMessageHandler messageHandler) throws IOException;

    /**
     * Execute an operation.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the future result of the operation
     */
    // TODO consider copying AsyncFuture and AsyncFutureTask into controller-client to eliminate the jboss-threads dependency
    AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler);

    /**
     * Execute an operation.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the future result of the operation
     */
    AsyncFuture<ModelNode> executeAsync(NewOperation operation, OperationMessageHandler messageHandler);

    class Factory {
        /**
         * Create a client instance for an existing channel. It is the client's responsibility to close this channel
         *
         * @param channel The channel to use
         * @param executorService
         * @return A model controller client
         * @throws UnknownHostException if the host cannot be found
         */
        public static NewModelControllerClient create(final ManagementChannel channel) {
            return new NewExistingChannelModelControllerClient(channel);
        }

        /**
         * Create a client instance for a remote address and port.
         *
         * @param address the address of the remote host
         * @param executorService
         * @return A model controller client
         * @throws UnknownHostException if the host cannot be found
         */
        public static NewModelControllerClient create(final InetAddress address, final int port){
            return new NewEstablishChannelModelControllerClient(address, port);
        }

        /**
         * Create a client instance for a remote address and port.
         *
         * @param hostName the remote host
         * @param executorService
         * @return A model controller client
         * @throws UnknownHostException if the host cannot be found
         */
        public static NewModelControllerClient create(final String hostName, final int port){
            return new NewEstablishChannelModelControllerClient(hostName, port);
        }
    }

}

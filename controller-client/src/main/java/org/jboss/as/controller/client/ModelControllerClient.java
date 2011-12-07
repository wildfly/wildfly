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
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.impl.RemotingModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;

/**
 * A client for an application server management model controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ModelControllerClient extends Closeable {

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
     * Note that associated input-streams have to be closed by the caller, after the
     * operation completed {@link OperationAttachments#isAutoCloseStreams()}.
     *
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode execute(Operation operation) throws IOException;

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
     * Note that associated input-streams have to be closed by the caller, after the
     * operation completed {@link OperationAttachments#isAutoCloseStreams()}.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException;

    /**
     * Execute an operation.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the future result of the operation
     */
    AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler);

    /**
     * Execute an operation.
     *
     * Note that associated input-streams have to be closed by the caller, after the
     * operation completed {@link OperationAttachments#isAutoCloseStreams()}.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the future result of the operation
     */
    AsyncFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler);

    class Factory {

        /**
         * Create a client instance for a remote address and port.
         *
         * @param address the address of the remote host
         * @param port the port
         * @return A model controller client
         */
        public static ModelControllerClient create(final InetAddress address, final int port){
            return new RemotingModelControllerClient(address.getHostName(), port, null, null);
        }

        /**
         * Create a client instance for a remote address and port.
         *
         * @param address the address of the remote host
         * @param port the port
         * @param handler  CallbackHandler to obtain authentication information for the call.
         * @return A model controller client
         */
        public static ModelControllerClient create(final InetAddress address, final int port, final CallbackHandler handler){
            return new RemotingModelControllerClient(address.getHostName(), port, handler, null);
        }

        /**
         * Create a client instance for a remote address and port.
         *
         * @param address the address of the remote host
         * @param port the port
         * @param handler  CallbackHandler to obtain authentication information for the call.
         * @param saslOptions Additional options to be passed to the SASL mechanism.
         * @return A model controller client
         */
        public static ModelControllerClient create(final InetAddress address, final int port, final CallbackHandler handler, final Map<String, String> saslOptions){
            return new RemotingModelControllerClient(address.getHostName(), port, handler, saslOptions);
        }

        /**
         * Create a client instance for a remote address and port.
         *
         * @param hostName the remote host
         * @param port the port
         * @return A model controller client
         * @throws UnknownHostException if the host cannot be found
         */
        public static ModelControllerClient create(final String hostName, final int port) throws UnknownHostException {
            return new RemotingModelControllerClient(hostName, port, null, null);
        }

        /**
         * Create a client instance for a remote address and port and CallbackHandler.
         *
         * @param hostName the remote host
         * @param port     the port
         * @param handler  CallbackHandler to obtain authentication information for the call.
         * @return A model controller client
         * @throws UnknownHostException if the host cannot be found
         */
        public static ModelControllerClient create(final String hostName, final int port, final CallbackHandler handler) throws UnknownHostException {
            return new RemotingModelControllerClient(hostName, port, handler, null);
        }

        /**
         * Create a client instance for a remote address and port and CallbackHandler.
         *
         * @param hostName the remote host
         * @param port     the port
         * @param handler  CallbackHandler to obtain authentication information for the call.
         * @param saslOptions Additional options to be passed to the SASL mechanism.
         * @return A model controller client
         * @throws UnknownHostException if the host cannot be found
         */
        public static ModelControllerClient create(final String hostName, final int port, final CallbackHandler handler, final Map<String, String> saslOptions) throws UnknownHostException {
            return new RemotingModelControllerClient(hostName, port, handler, saslOptions);
        }
    }

}

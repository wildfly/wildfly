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
import java.net.InetAddress;
import java.util.concurrent.CancellationException;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ModelControllerClient extends Closeable{

    /**
     * Execute an operation, possibly asynchronously, sending updates and the final result to the given handler.
     *
     * @param operation the operation to execute
     * @param handler the result handler
     * @return a handle which may be used to cancel the operation
     */
    Operation execute(ModelNode operation, ResultHandler handler);

    /**
     * Execute an operation synchronously.
     *
     * @param operation the operation to execute
     * @return the result
     * @throws CancellationException if the operation was cancelled due to interruption (the thread's interrupt
     * status will be set)
     */
    ModelNode execute(ModelNode operation) throws CancellationException;

    /**
     * A handle for a specific running operation.
     */
    interface Operation {

        /**
         * Attempt to cancel this operation.
         */
        void cancel();
    }

    class Factory {
        /**
         * Create an {@link org.jboss.as.PrototypeDetypedClient.client.api.StandaloneClient} instance for a remote address and port.
         *
         +         * @param address The remote address to connect to
         * @param port The remote port
         * @return A domain client
         */
        public static ModelControllerClient create(final InetAddress address, int port) {
            return new ModelControllerClientImpl(address, port);
        }
    }
}

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
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;

/**
 * A client for an application server management model controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface NewModelControllerClient extends Closeable {

    /**
     * Execute an operation.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the future result of the operation
     */
    AsyncFuture<ModelNode> executeOperationAsync(ModelNode operation, OperationMessageHandler messageHandler);

    /**
     * Execute an operation synchronously.
     *
     * @param operation the operation to execute
     * @param messageHandler the message handler to use for operation progress reporting, or {@code null} for none
     * @return the result of the operation
     * @throws IOException if an I/O error occurs while executing the operation
     */
    ModelNode executeOperation(ModelNode operation, OperationMessageHandler messageHandler) throws IOException;
}

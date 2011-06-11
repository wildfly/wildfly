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

package org.jboss.as.domain.controller;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.ModelController} that manages a given host's {@link org.jboss.as.domain.controller.DomainController}'s
 * copy of the domain-wide management model, it's own host-wide model and its
 * proxy connections to the host's servers.
 *
 * @author Emanuel Muckenhuber
 */
public interface NewDomainModel extends NewModelController {

    /**
     * Execute the given operation against the local domain-wide and host-wide management
     * model, returning the result as well as information about what operations are needed
     * to effect the operation on the servers managed by this host.
     *
     * @param operation the operation to execute
     * @param handler the message handler
     * @param control the transaction control for this operation
     * @param attachments the operation attachments
     *
     * @return the operation result included information on how to apply the operation to
     *         the servers managed by this host
     */
    ModelNode executeForDomain(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments);

    /**
     * Get a snapshot of the underlying domain-wide model.
     *
     * @return the model.
     */
    ModelNode getDomainModel();

}

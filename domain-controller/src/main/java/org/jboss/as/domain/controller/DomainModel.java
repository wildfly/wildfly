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

import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.TransactionalModelController;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

/**
 * {@link TransactionalModelController} that manages a given host's {@link DomainController}'s
 * copy of the domain-wide management model, it's own host-wide model and its
 * proxy connections to the host's servers.
 *
 * @author Emanuel Muckenhuber
 */
public interface DomainModel extends TransactionalModelController {

    /**
     * Execute the given operation against the local domain-wide and host-wide management
     * model, returning the result as well as information about what operations are needed
     * to effect the operation on the servers managed by this host.
     *
     * @param operation the operation
     * @param transaction transactional context in which the operation should be executed. Changes
     *                    made by {@code operation} should not be visible to other threads until
     *                    the transaction is committed.
     *
     * @return the operation result included information on how to apply the operation to
     *         the servers managed by this host
     */
    ModelNode executeForDomain(final Operation operation, final ControllerTransactionContext transaction);

    /**
     * Get a snapshot of the underlying domain-wide model.
     *
     * @return the model.
     */
    ModelNode getDomainModel();

}

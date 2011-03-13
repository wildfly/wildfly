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
package org.jboss.as.controller;

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;


/**
 * A {@link ModelController} that can participate in a simple one-phase
 * commit "transaction"only applying changes to the model
 * when the transaction is committed.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface TransactionalModelController extends ModelController {

    /**
     * Execute an operation synchronously, but only apply any changes to the model
     * upon transaction commit.
     *
     * @param operation the operation to execute
     * @param transaction the transaction. Will not be {@code null}
     *
     * @return the result
     * @throws CancellationException if the operation was cancelled due to interruption (the thread's interrupt
     * status will be set)
     */
    ModelNode execute(Operation operation, ControllerTransactionContext transaction);

    /**
     * Execute an operation, possibly asynchronously, sending updates and the final result to the given handler,
     * but only apply any changes to the model upon transaction commit.
     *
     * @param operation the operation to execute
     * @param handler the result handler
     * @param transaction the transaction. Will not be {@code null}
     *
     * @return a handle which may be used to cancel the operation or obtain a compensating operation
     */
    OperationResult execute(Operation operation, ResultHandler handler, ControllerTransactionContext transaction);
}

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

import org.jboss.as.controller.SynchronousOperationSupport.AsynchronousOperationController;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass for {@link ModelController} implementations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelController<T> implements ModelController, AsynchronousOperationController<T> {


    /** {@inheritDoc} */
    @Override
    public ModelNode execute(final Operation operation) {
        return SynchronousOperationSupport.execute(operation, getOperationControllerContext(operation), this);
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler) {
        return execute(operation, handler, getOperationControllerContext(operation));
    }

    /**
     * Gets the handback object to pass to {@link #execute(Operation, ResultHandler, Object)}
     *
     * @param operation the operation being executed
     *
     * @return the handback
     */
    protected abstract T getOperationControllerContext(Operation operation);

    /**
     * Get a failure result from a throwable exception.
     *
     * @param t the exception
     * @return the failure result
     */
    protected ModelNode getFailureResult(Throwable t) {
        final ModelNode node = new ModelNode();
        // todo - define this structure
        do {
            final String message = t.getLocalizedMessage();
            node.add(t.getClass().getName(), message != null ? message : "");
            t = t.getCause();
        } while (t != null);
        return node;
    }

}

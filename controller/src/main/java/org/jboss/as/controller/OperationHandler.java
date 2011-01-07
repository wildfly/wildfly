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

import org.jboss.dmr.ModelNode;

/**
 * An operation handler.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface OperationHandler {

    /**
     * Get the description for this operation, without address information.
     *
     * @return the description
     */
    ModelNode getOperationDescription();

    /**
     * Execute an operation.  This method <b>must</b> invoke one of the completion methods on {@code resultHandler}
     * regardless of the outcome of the operation.  This method returns an operation which would reverse
     * (undo) this operation, if possible.
     *
     * @param context the context for this operation
     * @param operation the operation being executed
     * @param resultHandler the result handler to invoke when the operation is complete
     * @return the compensating operation
     */
    ModelNode execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler);
}

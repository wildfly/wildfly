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

package org.jboss.as.server;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;

/**
 * An operation handler which applies to a server.  For any given operation, if the operation handler in question
 * also implements {@link org.jboss.as.controller.ModelQueryOperationHandler} or one of its subtypes, either of the
 * {@code execute} methods may be invoked depending on the state of the server, or whether the operation is running
 * on the domain or host controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServerOperationHandler extends OperationHandler {

    /**
     * Execute an operation, also applying it to the runtime environment.  This method <b>must</b> invoke one of the
     * completion methods on {@code resultHandler} regardless of the outcome of the operation.
     *
     *
     * @param context the context for this operation
     * @param operation the operation being executed
     * @param resultHandler the result handler to invoke when the operation is complete
     * @return a handle which may be used to asynchronously cancel this operation or to retrieve the compensating operation
     */
    OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException;
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.THREAD_POOL;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class NewRemotingSubsystemAdd implements ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new NewRemotingSubsystemAdd();

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        context.getSubModel().get(THREAD_POOL).set(operation.require(REQUEST_PROPERTIES).require(THREAD_POOL));
        // initialize the connectors
        context.getSubModel().get(CONNECTOR).setEmptyObject();

        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensating.get(OP).set(REMOVE);

        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }
}

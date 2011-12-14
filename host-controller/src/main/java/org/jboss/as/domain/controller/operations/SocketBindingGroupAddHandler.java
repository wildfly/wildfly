/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.domain.controller.operations;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.AbstractSocketBindingGroupAddHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the domain socket-binding-group resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class SocketBindingGroupAddHandler extends AbstractSocketBindingGroupAddHandler {

    public static final SocketBindingGroupAddHandler INSTANCE = new SocketBindingGroupAddHandler();

    private SocketBindingGroupAddHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);
        /* This will be reintroduced for 7.2.0, leave commented out
        SocketBindingGroupResourceDefinition.INCLUDES.validateAndSet(operation, model);
        */
    }

}

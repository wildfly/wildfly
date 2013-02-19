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

package org.jboss.as.domain.controller.resources;

import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingRemoveHandler;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a domain-level socket binding resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingResourceDefinition extends AbstractSocketBindingResourceDefinition {

    public static final SocketBindingResourceDefinition INSTANCE = new SocketBindingResourceDefinition();

    private static final OperationStepHandler SHARED_HANDLER = new ModelOnlyWriteAttributeHandler(
            AbstractSocketBindingResourceDefinition.INTERFACE,
            AbstractSocketBindingResourceDefinition.PORT,
            AbstractSocketBindingResourceDefinition.FIXED_PORT,
            AbstractSocketBindingResourceDefinition.MULTICAST_ADDRESS,
            AbstractSocketBindingResourceDefinition.MULTICAST_PORT,
            AbstractSocketBindingResourceDefinition.CLIENT_MAPPINGS
    );

    private SocketBindingResourceDefinition() {
        super(SocketBindingAddHandler.INSTANCE, SocketBindingRemoveHandler.INSTANCE);
    }

    @Override
    protected OperationStepHandler getInterfaceWriteAttributeHandler() {
        return SHARED_HANDLER;
    }

    @Override
    protected OperationStepHandler getPortWriteAttributeHandler() {
        return SHARED_HANDLER;
    }

    @Override
    protected OperationStepHandler getFixedPortWriteAttributeHandler() {
        return SHARED_HANDLER;
    }

    @Override
    protected OperationStepHandler getMulticastAddressWriteAttributeHandler() {
        return SHARED_HANDLER;
    }

    @Override
    protected OperationStepHandler getMulticastPortWriteAttributeHandler() {
        return SHARED_HANDLER;
    }

    @Override
    protected OperationStepHandler getClientMappingsWriteAttributeHandler() {
        return SHARED_HANDLER;
    }
}

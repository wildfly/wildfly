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

package org.jboss.as.domain.controller.resource;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingRemoveHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;

/**
 * {@link ResourceDefinition} for a domain-level socket binding resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingResourceDefinition extends AbstractSocketBindingResourceDefinition {

    public static final SocketBindingResourceDefinition INSTANCE = new SocketBindingResourceDefinition();

    private static final OperationStepHandler INTERFACE_HANDLER =
            new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(AbstractSocketBindingResourceDefinition.INTERFACE);

    private static final OperationStepHandler PORT_HANDLER =
            new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(AbstractSocketBindingResourceDefinition.PORT);

    private static final OperationStepHandler FIXED_PORT_HANDLER =
            new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(AbstractSocketBindingResourceDefinition.FIXED_PORT);

    private static final OperationStepHandler MULTICAST_ADDRESS_HANDLER =
            new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(AbstractSocketBindingResourceDefinition.MULTICAST_ADDRESS);

    private static final OperationStepHandler MULTICAST_PORT_HANDLER =
            new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(AbstractSocketBindingResourceDefinition.MULTICAST_PORT);

    private static final OperationStepHandler CLIENT_MAPPINGS_HANDLER =
            new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(AbstractSocketBindingResourceDefinition.CLIENT_MAPPINGS);

    private SocketBindingResourceDefinition() {
        super(SocketBindingAddHandler.INSTANCE, SocketBindingRemoveHandler.INSTANCE);
    }

    @Override
    protected OperationStepHandler getInterfaceWriteAttributeHandler() {
        return INTERFACE_HANDLER;
    }

    @Override
    protected OperationStepHandler getPortWriteAttributeHandler() {
        return PORT_HANDLER;
    }

    @Override
    protected OperationStepHandler getFixedPortWriteAttributeHandler() {
        return FIXED_PORT_HANDLER;
    }

    @Override
    protected OperationStepHandler getMulticastAddressWriteAttributeHandler() {
        return MULTICAST_ADDRESS_HANDLER;
    }

    @Override
    protected OperationStepHandler getMulticastPortWriteAttributeHandler() {
        return MULTICAST_PORT_HANDLER;
    }

    @Override
    protected OperationStepHandler getClientMappingsWriteAttributeHandler() {
        return CLIENT_MAPPINGS_HANDLER;
    }
}
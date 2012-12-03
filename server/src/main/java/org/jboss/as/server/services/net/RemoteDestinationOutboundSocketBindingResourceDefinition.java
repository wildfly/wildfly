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

package org.jboss.as.server.services.net;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource definition for a remote-destination outbound socket binding
 *
 * @author Jaikiran Pai
 */
public class RemoteDestinationOutboundSocketBindingResourceDefinition extends OutboundSocketBindingResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING);

    public static final RemoteDestinationOutboundSocketBindingResourceDefinition INSTANCE = new RemoteDestinationOutboundSocketBindingResourceDefinition();

    public static final SimpleAttributeDefinition HOST = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST, ModelType.STRING, false)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT, false)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(0, 65535, false, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition[] ATTRIBUTES = {HOST, PORT, SOURCE_INTERFACE, SOURCE_PORT, FIXED_SOURCE_PORT};

    private RemoteDestinationOutboundSocketBindingResourceDefinition() {
        super(PATH,
                ControllerResolver.getResolver(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING),
                RemoteDestinationOutboundSocketBindingAddHandler.INSTANCE,
                OutboundSocketBindingRemoveHandler.INSTANCE, true);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(HOST, null, new OutboundSocketBindingWriteHandler(HOST, true));
        resourceRegistration.registerReadWriteAttribute(PORT, null, new OutboundSocketBindingWriteHandler(PORT, true));
    }
}

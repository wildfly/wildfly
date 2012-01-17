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

package org.jboss.as.server.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.operations.HttpManagementAddHandler;
import org.jboss.as.server.operations.HttpManagementRemoveHandler;
import org.jboss.as.server.operations.HttpManagementWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the HTTP management interface resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HttpManagementResourceDefinition extends SimpleResourceDefinition {

    private static final PathElement RESOURCE_PATH = PathElement.pathElement(MANAGEMENT_INTERFACE, HTTP_INTERFACE);

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURITY_REALM, ModelType.STRING, true)
                .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
                .build();

    public static final SimpleAttributeDefinition INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INTERFACE, ModelType.STRING, false)
                .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                .setAlternatives(ModelDescriptionConstants.SOCKET_BINDING, ModelDescriptionConstants.SECURE_SOCKET_BINDING)
                .build();

    public static final SimpleAttributeDefinition HTTP_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .setAlternatives(ModelDescriptionConstants.SOCKET_BINDING, ModelDescriptionConstants.SECURE_SOCKET_BINDING)
            .setRequires(ModelDescriptionConstants.INTERFACE)
            .build();

    public static final SimpleAttributeDefinition HTTPS_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURE_PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .setAlternatives(ModelDescriptionConstants.SOCKET_BINDING, ModelDescriptionConstants.SECURE_SOCKET_BINDING)
            .setRequires(ModelDescriptionConstants.INTERFACE)
            .build();

    public static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.HTTP.getLocalName())
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAlternatives(ModelDescriptionConstants.INTERFACE)
            .build();

    public static final SimpleAttributeDefinition SECURE_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURE_SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.HTTPS.getLocalName())
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAlternatives(ModelDescriptionConstants.INTERFACE)
            .build();

    public static final SimpleAttributeDefinition CONSOLE_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CONSOLE_ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.CONSOLE_ENABLED.getLocalName())
            .setDefaultValue(new ModelNode(true))
            .build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = new AttributeDefinition[] {INTERFACE, HTTP_PORT, HTTPS_PORT, SECURITY_REALM, SOCKET_BINDING, SECURE_SOCKET_BINDING,CONSOLE_ENABLED};

    public static final HttpManagementResourceDefinition INSTANCE = new HttpManagementResourceDefinition();

    private HttpManagementResourceDefinition() {
        super(RESOURCE_PATH,
                ServerDescriptions.getResourceDescriptionResolver("core.management.http-interface"),
                HttpManagementAddHandler.INSTANCE, HttpManagementRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTE_DEFINITIONS) {
            resourceRegistration.registerReadWriteAttribute(attr, null, HttpManagementWriteAttributeHandler.INSTANCE);
        }
    }
}

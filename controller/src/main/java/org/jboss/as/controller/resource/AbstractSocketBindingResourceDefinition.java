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

package org.jboss.as.controller.resource;

import java.util.Locale;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a resource representing a socket binding.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractSocketBindingResourceDefinition extends SimpleResourceDefinition {

    // Common attributes

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1)).build();

    public static final SimpleAttributeDefinition INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INTERFACE, ModelType.STRING, true)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final SimpleAttributeDefinition PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT, false)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, false, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final SimpleAttributeDefinition FIXED_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.FIXED_PORT, ModelType.BOOLEAN, true)
            .setAllowExpression(true).setDefaultValue(new ModelNode().set(false)).build();

    public static final SimpleAttributeDefinition MULTICAST_ADDRESS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MULTICAST_ADDRESS, ModelType.STRING, true)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final SimpleAttributeDefinition MULTICAST_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MULTICAST_PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(1, 65535, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final ClientMappingsAttributeDefinition CLIENT_MAPPINGS = new ClientMappingsAttributeDefinition(ModelDescriptionConstants.CLIENT_MAPPINGS);

    public AbstractSocketBindingResourceDefinition(final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        super(PathElement.pathElement(ModelDescriptionConstants.SOCKET_BINDING),
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.SOCKET_BINDING),
                addHandler, removeHandler, OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(NAME, null);
        resourceRegistration.registerReadWriteAttribute(INTERFACE, null, getInterfaceWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(PORT, null, getPortWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(FIXED_PORT, null, getFixedPortWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(MULTICAST_ADDRESS, null, getMulticastAddressWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(MULTICAST_PORT, null, getMulticastPortWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(CLIENT_MAPPINGS, null, getClientMappingsWriteAttributeHandler());

    }

    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler,
                                        OperationEntry.Flag... flags) {
        DescriptionProvider provider = new DefaultResourceAddDescriptionProvider(registration, getResourceDescriptionResolver()) {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                // "name" is not an operation parameter
                final ModelNode result = super.getModelDescription(locale);
                if (result.get(ModelDescriptionConstants.REQUEST_PROPERTIES).hasDefined(NAME.getName())) {
                    result.get(ModelDescriptionConstants.REQUEST_PROPERTIES).remove(NAME.getName());
                }
                return result;
            }
        };
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, handler, provider, getFlagsSet(flags));
    }

    protected abstract OperationStepHandler getInterfaceWriteAttributeHandler();
    protected abstract OperationStepHandler getPortWriteAttributeHandler();
    protected abstract OperationStepHandler getFixedPortWriteAttributeHandler();
    protected abstract OperationStepHandler getMulticastAddressWriteAttributeHandler();
    protected abstract OperationStepHandler getMulticastPortWriteAttributeHandler();
    protected abstract OperationStepHandler getClientMappingsWriteAttributeHandler();
}

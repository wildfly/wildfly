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

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
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
 * {@link ResourceDefinition} for a resource representing a socket binding group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingGroupResourceDefinition extends SimpleResourceDefinition {

    // Common attributes

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1)).build();

    public static final SimpleAttributeDefinition DEFAULT_INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DEFAULT_INTERFACE, ModelType.STRING, false)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    // Server-only attributes.

    public static final SimpleAttributeDefinition PORT_OFFSET = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT_OFFSET, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .setDefaultValue(new ModelNode().set(0)).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    // Domain-only attributes

    public static final ListAttributeDefinition INCLUDES = SocketBindingGroupIncludesAttribute.INSTANCE;

    private final boolean forDomainModel;

    public SocketBindingGroupResourceDefinition(final OperationStepHandler addHandler, final OperationStepHandler removeHandler, final boolean forDomainModel) {
        super(PathElement.pathElement(ModelDescriptionConstants.SOCKET_BINDING_GROUP),
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.SOCKET_BINDING_GROUP),
                addHandler, removeHandler, OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.forDomainModel = forDomainModel;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        resourceRegistration.registerReadOnlyAttribute(NAME, null);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_INTERFACE, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_INTERFACE));

        if (forDomainModel) {
            /* This will be reintroduced for 7.2.0, leave commented out
            resourceRegistration.registerReadWriteAttribute(INCLUDES, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(INCLUDES));
            */
        } else {
            resourceRegistration.registerReadWriteAttribute(PORT_OFFSET, null, new ReloadRequiredWriteAttributeHandler(PORT_OFFSET));
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        /* This will be reintroduced for 7.2.0, leave commented out
        if (forDomainModel) {
            resourceRegistration.registerOperationHandler(SocketBindingGroupIncludeAddHandler.OPERATION_NAME, SocketBindingGroupIncludeAddHandler.INSTANCE, SocketBindingGroupIncludeAddHandler.INSTANCE);
            resourceRegistration.registerOperationHandler(SocketBindingGroupIncludeRemoveHandler.OPERATION_NAME, SocketBindingGroupIncludeRemoveHandler.INSTANCE, SocketBindingGroupIncludeRemoveHandler.INSTANCE);
        }
        */
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

}

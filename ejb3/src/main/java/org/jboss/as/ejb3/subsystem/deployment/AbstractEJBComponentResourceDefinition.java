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

package org.jboss.as.ejb3.subsystem.deployment;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * Base class for {@link ResourceDefinition}s describing runtime {@link EJBComponent}s.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractEJBComponentResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition COMPONENT_CLASS_NAME = new SimpleAttributeDefinitionBuilder("component-class-name", ModelType.STRING)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder("security-domain", ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    public static final SimpleAttributeDefinition RUN_AS_ROLE = new SimpleAttributeDefinitionBuilder("run-as-role", ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final ListAttributeDefinition DECLARED_ROLES = new ListAttributeDefinition("declared-roles", true,
            new StringLengthValidator(1), AttributeAccess.Flag.STORAGE_RUNTIME) {
        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            throw MESSAGES.runtimeAttributeNotMarshallable(getName());
        }
    };

    // Pool attributes

    public static final SimpleAttributeDefinition POOL_AVAILABLE_COUNT = new SimpleAttributeDefinitionBuilder("pool-available-count", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    public static final SimpleAttributeDefinition POOL_CREATE_COUNT = new SimpleAttributeDefinitionBuilder("pool-create-count", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    public static final SimpleAttributeDefinition POOL_CURRENT_SIZE = new SimpleAttributeDefinitionBuilder("pool-current-size", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    public static final SimpleAttributeDefinition POOL_NAME = new SimpleAttributeDefinitionBuilder("pool-name", ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    public static final SimpleAttributeDefinition POOL_REMOVE_COUNT = new SimpleAttributeDefinitionBuilder("pool-remove-count", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    public static final SimpleAttributeDefinition POOL_MAX_SIZE = new SimpleAttributeDefinitionBuilder("pool-max-size", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();

    private final EJBComponentType componentType;

    public AbstractEJBComponentResourceDefinition(final EJBComponentType componentType) {
        super(PathElement.pathElement(componentType.getResourceType()),
                EJB3Extension.getResourceDescriptionResolver(componentType.getResourceType()));
        this.componentType = componentType;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final AbstractEJBComponentRuntimeHandler<?> handler = componentType.getRuntimeHandler();
        resourceRegistration.registerReadOnlyAttribute(COMPONENT_CLASS_NAME, handler);
        resourceRegistration.registerReadOnlyAttribute(SECURITY_DOMAIN, handler);
        resourceRegistration.registerReadOnlyAttribute(RUN_AS_ROLE, handler);
        resourceRegistration.registerReadOnlyAttribute(DECLARED_ROLES, handler);
        if (componentType.hasTimer()) {
            resourceRegistration.registerReadOnlyAttribute(TimerAttributeDefinition.INSTANCE, handler);
        }

        if (componentType.hasPool()) {
            resourceRegistration.registerReadOnlyAttribute(POOL_AVAILABLE_COUNT, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_CREATE_COUNT, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_NAME, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_REMOVE_COUNT, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_CURRENT_SIZE, handler);
            resourceRegistration.registerReadWriteAttribute(POOL_MAX_SIZE, handler, handler);
        }
    }
}

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

package org.jboss.as.naming.subsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for JNDI bindings
 */
public class NamingBindingResourceDefinition extends SimpleResourceDefinition {

    static final NamingBindingResourceDefinition INSTANCE = new NamingBindingResourceDefinition();

    static final SimpleAttributeDefinition BINDING_TYPE = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.BINDING_TYPE, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(EnumValidator.create(BindingType.class, false, false))
            .build();

    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.VALUE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.CLASS, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.MODULE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final SimpleAttributeDefinition LOOKUP = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.LOOKUP, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final PropertiesAttributeDefinition ENVIRONMENT = new PropertiesAttributeDefinition.Builder(NamingSubsystemModel.ENVIRONMENT, true)
            .setXmlName("property")
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition CACHE = new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.CACHE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {BINDING_TYPE, VALUE, TYPE, CLASS, MODULE, LOOKUP, ENVIRONMENT, CACHE};


    private static final List<AccessConstraintDefinition> ACCESS_CONSTRAINTS;
    static {
        List<AccessConstraintDefinition> constraints =  new ArrayList<AccessConstraintDefinition>();
        constraints.add(NamingExtension.NAMING_BINDING_APPLICATION_CONSTRAINT);
        constraints.add(NamingExtension.NAMING_BINDING_SENSITIVITY_CONSTRAINT);
        ACCESS_CONSTRAINTS = Collections.unmodifiableList(constraints);
    }

    private NamingBindingResourceDefinition() {
        super(NamingSubsystemModel.BINDING_PATH,
                NamingExtension.getResourceDescriptionResolver(NamingSubsystemModel.BINDING),
                NamingBindingAdd.INSTANCE, NamingBindingRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new WriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return ACCESS_CONSTRAINTS;
    }

    private static class WriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        private WriteAttributeHandler(AttributeDefinition... definitions) {
            super(definitions);
        }
        @Override
        protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
            super.validateUpdatedModel(context, model);
            validateResourceModel(model.getModel());
        }
    }

    static void validateResourceModel(ModelNode modelNode) throws OperationFailedException {
        final BindingType type = BindingType.forName(modelNode.require(NamingSubsystemModel.BINDING_TYPE).asString());
        if (type == BindingType.SIMPLE) {
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.VALUE.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.VALUE.getName());
            }
            if (modelNode.hasDefined(NamingBindingResourceDefinition.CACHE.getName())) {
                throw NamingLogger.ROOT_LOGGER.cacheNotValidForBindingType(type);
            }
        } else if (type == BindingType.OBJECT_FACTORY) {
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.MODULE.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.MODULE.getName());
            }
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.CLASS.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.CLASS.getName());
            }
            if (modelNode.hasDefined(NamingBindingResourceDefinition.CACHE.getName())) {
                throw NamingLogger.ROOT_LOGGER.cacheNotValidForBindingType(type);
            }
        } else if (type == BindingType.EXTERNAL_CONTEXT) {
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.MODULE.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.MODULE.getName());
            }
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.CLASS.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.CLASS.getName());
            }
        } else if (type == BindingType.LOOKUP) {
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.LOOKUP.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.LOOKUP.getName());
            }
            if (modelNode.hasDefined(NamingBindingResourceDefinition.CACHE.getName())) {
                throw NamingLogger.ROOT_LOGGER.cacheNotValidForBindingType(type);
            }
        } else {
            throw NamingLogger.ROOT_LOGGER.unknownBindingType(type.toString());
        }
    }
}

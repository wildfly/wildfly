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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.service.BinderService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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

    static final OperationStepHandler VALIDATE_RESOURCE_MODEL_OPERATION_STEP_HANDLER = (context, op) -> validateResourceModel(context.readResource(PathAddress.EMPTY_ADDRESS).getModel(), true);

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
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(NamingSubsystemModel.REBIND, getResourceDescriptionResolver())
                // disallow rebind op for external-context
                .addParameter(SimpleAttributeDefinitionBuilder.create(BINDING_TYPE)
                        .setValidator(new EnumValidator<>(BindingType.class, EnumSet.complementOf(EnumSet.of(BindingType.EXTERNAL_CONTEXT))))
                        .build())
                .addParameter(TYPE)
                .addParameter(VALUE)
                .addParameter(CLASS)
                .addParameter(MODULE)
                .addParameter(LOOKUP)
                .addParameter(ENVIRONMENT);
        resourceRegistration.registerOperationHandler(builder.build(), new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                        Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
                        ModelNode model = resource.getModel();
                        for (AttributeDefinition attr : ATTRIBUTES) {
                            attr.validateAndSet(operation, model);
                        }

                        context.addStep(NamingBindingResourceDefinition.VALIDATE_RESOURCE_MODEL_OPERATION_STEP_HANDLER, OperationContext.Stage.MODEL);

                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                final String name = context.getCurrentAddressValue();
                                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
                                ServiceController<ManagedReferenceFactory> service = (ServiceController<ManagedReferenceFactory>) context.getServiceRegistry(false).getService(bindInfo.getBinderServiceName());
                                if (service == null) {
                                    context.reloadRequired();
                                    return;
                                }
                                NamingBindingAdd.INSTANCE.doRebind(context, operation, (BinderService) service.getService());
                            }
                        }, OperationContext.Stage.RUNTIME);
                    }
                }, OperationContext.Stage.MODEL);
            }
        });
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
            context.addStep(VALIDATE_RESOURCE_MODEL_OPERATION_STEP_HANDLER, OperationContext.Stage.MODEL);
        }
    }

    static void validateResourceModel(ModelNode modelNode, boolean allowExternal) throws OperationFailedException {
        final BindingType type = BindingType.forName(modelNode.require(NamingSubsystemModel.BINDING_TYPE).asString());
        if (type == BindingType.SIMPLE) {
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.VALUE.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.VALUE.getName());
            }
            if (modelNode.hasDefined(NamingBindingResourceDefinition.CACHE.getName())
                    && modelNode.get(NamingBindingResourceDefinition.CACHE.getName()).asBoolean()) {
                throw NamingLogger.ROOT_LOGGER.cacheNotValidForBindingType(type);
            }
        } else if (type == BindingType.OBJECT_FACTORY) {
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.MODULE.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.MODULE.getName());
            }
            if(!modelNode.hasDefined(NamingBindingResourceDefinition.CLASS.getName())) {
                throw NamingLogger.ROOT_LOGGER.bindingTypeRequiresAttributeDefined(type, NamingBindingResourceDefinition.CLASS.getName());
            }
            if (modelNode.hasDefined(NamingBindingResourceDefinition.CACHE.getName())
                    && modelNode.get(NamingBindingResourceDefinition.CACHE.getName()).asBoolean()) {
                throw NamingLogger.ROOT_LOGGER.cacheNotValidForBindingType(type);
            }
        } else if (type == BindingType.EXTERNAL_CONTEXT) {
            if(!allowExternal) {
                throw NamingLogger.ROOT_LOGGER.cannotRebindExternalContext();
            }
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
            if (modelNode.hasDefined(NamingBindingResourceDefinition.CACHE.getName())
                    && modelNode.get(NamingBindingResourceDefinition.CACHE.getName()).asBoolean()) {
                throw NamingLogger.ROOT_LOGGER.cacheNotValidForBindingType(type);
            }
        } else {
            throw NamingLogger.ROOT_LOGGER.unknownBindingType(type.toString());
        }
    }
}

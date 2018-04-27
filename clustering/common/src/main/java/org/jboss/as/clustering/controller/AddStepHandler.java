/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Generic add operation step handler that delegates service installation/rollback to a {@link ResourceServiceHandler}.
 * @author Paul Ferraro
 */
public class AddStepHandler extends AbstractAddStepHandler implements Registration<ManagementResourceRegistration>, DescribedAddStepHandler {

    private final AddStepHandlerDescriptor descriptor;
    private final ResourceServiceHandler handler;

    public AddStepHandler(AddStepHandlerDescriptor descriptor) {
        this(descriptor, null);
    }

    public AddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        this.descriptor = descriptor;
        this.handler = handler;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return super.requiresRuntime(context) && (this.handler != null);
    }

    @Override
    public AddStepHandlerDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress parentAddress = address.getParent();
        PathElement path = address.getLastElement();

        OperationStepHandler parentHandler = context.getRootResourceRegistration().getOperationHandler(parentAddress, ModelDescriptionConstants.ADD);
        if (parentHandler instanceof DescribedAddStepHandler) {
            AddStepHandlerDescriptor parentDescriptor = ((DescribedAddStepHandler) parentHandler).getDescriptor();

            if (parentDescriptor.getRequiredChildren().contains(path)) {
                if (context.readResourceFromRoot(parentAddress, false).hasChild(path)) {
                    // If we are a required child resource of our parent, we need to remove the auto-created resource first
                    context.addStep(Util.createRemoveOperation(address), context.getRootResourceRegistration().getOperationHandler(address, ModelDescriptionConstants.REMOVE), OperationContext.Stage.MODEL);
                    context.addStep(operation, this, OperationContext.Stage.MODEL);
                    return;
                }
            }
            for (PathElement requiredPath : parentDescriptor.getRequiredSingletonChildren()) {
                String requiredPathKey = requiredPath.getKey();
                if (requiredPath.getKey().equals(path.getKey())) {
                    Set<String> childrenNames = context.readResourceFromRoot(parentAddress, false).getChildrenNames(requiredPathKey);
                    if (!childrenNames.isEmpty()) {
                        // If there is a required singleton sibling resource, we need to remove it first
                        for (String childName : childrenNames) {
                            PathAddress singletonAddress = parentAddress.append(requiredPathKey, childName);
                            context.addStep(Util.createRemoveOperation(singletonAddress), context.getRootResourceRegistration().getOperationHandler(singletonAddress, ModelDescriptionConstants.REMOVE), OperationContext.Stage.MODEL);
                        }
                        context.addStep(operation, this, OperationContext.Stage.MODEL);
                        return;
                    }
                }
            }
        }

        super.execute(context, operation);

        if (this.requiresRuntime(context)) {
            for (RuntimeResourceRegistration registration : this.descriptor.getRuntimeResourceRegistrations()) {
                context.addStep(new RuntimeResourceRegistrationStepHandler(registration), OperationContext.Stage.MODEL);
            }
        }
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        // Validate extra add operation parameters
        for (AttributeDefinition definition : this.descriptor.getExtraParameters()) {
            definition.validateOperation(operation);
        }
        PathAddress currentAddress = context.getCurrentAddress();
        // Validate and apply attribute translations
        Map<AttributeDefinition, AttributeTranslation> translations = this.descriptor.getAttributeTranslations();
        for (Map.Entry<AttributeDefinition, AttributeTranslation> entry : translations.entrySet()) {
            AttributeDefinition sourceParameter = entry.getKey();
            AttributeTranslation translation = entry.getValue();
            if (operation.hasDefined(sourceParameter.getName())) {
                ModelNode value = sourceParameter.validateOperation(operation);
                ModelNode targetValue = translation.getWriteTranslator().translate(context, value);
                Attribute targetAttribute = translation.getTargetAttribute();
                PathAddress targetAddress = translation.getPathAddressTransformation().apply(currentAddress);
                // If target attribute exists in the current resource, just fix the operation
                // Otherwise, we need a separate write-attribute operation
                if (targetAddress == currentAddress) {
                    String targetName = targetAttribute.getName();
                    if (!operation.hasDefined(targetName)) {
                        operation.get(targetName).set(targetValue);
                    }
                } else {
                    ModelNode writeAttributeOperation = Operations.createWriteAttributeOperation(targetAddress, targetAttribute, targetValue);
                    ImmutableManagementResourceRegistration targetRegistration = translation.getResourceRegistrationTransformation().apply(context.getResourceRegistration());
                    OperationStepHandler writeAttributeHandler = targetRegistration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, targetAttribute.getName()).getWriteHandler();
                    context.addStep(writeAttributeOperation, writeAttributeHandler, OperationContext.Stage.MODEL);
                }
            }
        }
        // Validate proper attributes
        ModelNode model = resource.getModel();
        ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
        for (String attributeName : registration.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
            AttributeAccess attribute = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            AttributeDefinition definition = attribute.getAttributeDefinition();
            if ((attribute.getStorageType() == AttributeAccess.Storage.CONFIGURATION) && !translations.containsKey(definition)) {
                definition.validateAndSet(operation, model);
            }
        }

        // Auto-create required child resources as necessary
        addRequiredChildren(context, this.descriptor.getRequiredChildren(), (Resource parent, PathElement path) -> parent.hasChild(path));
        addRequiredChildren(context, this.descriptor.getRequiredSingletonChildren(), (Resource parent, PathElement path) -> parent.hasChildren(path.getKey()));
    }

    private static void addRequiredChildren(OperationContext context, Collection<PathElement> paths, BiPredicate<Resource, PathElement> present) {
        for (PathElement path : paths) {
            context.addStep(Util.createAddOperation(context.getCurrentAddress().append(path)), new AddIfAbsentStepHandler(present), OperationContext.Stage.MODEL);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        this.handler.installServices(context, model);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            this.handler.removeServices(context, resource.getModel());
        } catch (OperationFailedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        ModelNode model = resource.getModel();
        // The super implementation assumes that the capability name is a simple extension of the base name - we do not.
        // Only register capabilities when allowed by the associated predicate
        for (Map.Entry<Capability, Predicate<ModelNode>> entry : this.descriptor.getCapabilities().entrySet()) {
            if (entry.getValue().test(model)) {
                context.registerCapability(entry.getKey().resolve(address));
            }
        }

        ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
        for (String attributeName : registration.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
            AttributeDefinition attribute = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName).getAttributeDefinition();
            if (attribute.hasCapabilityRequirements()) {
                attribute.addCapabilityRequirements(context, resource, model.get(attributeName));
            }
        }

        for (CapabilityReferenceRecorder recorder : context.getResourceRegistration().getRequirements()) {
            recorder.addCapabilityRequirements(context, resource, null);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.descriptor.getDescriptionResolver()).withFlag(OperationEntry.Flag.RESTART_NONE);
        if (registration.isOrderedChildResource()) {
            builder.addParameter(SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ADD_INDEX, ModelType.INT, true).build());
        }
        for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
            builder.addParameter(attribute);
        }
        for (AttributeDefinition parameter : this.descriptor.getExtraParameters()) {
            builder.addParameter(parameter);
        }
        for (AttributeDefinition attribute : this.descriptor.getAttributeTranslations().keySet()) {
            builder.addParameter(attribute);
        }
        registration.registerOperationHandler(builder.build(), this.descriptor.getAddOperationTransformation().apply(this));
    }
}

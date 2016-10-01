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
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
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
    private final OperationStepHandler writeAttributeHandler;

    public AddStepHandler(AddStepHandlerDescriptor descriptor) {
        this(descriptor, null);
    }

    public AddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        this(descriptor, handler, new ReloadRequiredWriteAttributeHandler(descriptor));
    }

    AddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler, OperationStepHandler writeAttributeHandler) {
        super(descriptor.getAttributes());
        this.descriptor = descriptor;
        this.handler = handler;
        this.writeAttributeHandler = writeAttributeHandler;
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
            } else {
                Optional<PathElement> singletonPathResult = parentDescriptor.getRequiredSingletonChildren().stream().filter((PathElement requiredPath) -> requiredPath.getKey().equals(path.getKey()) && !requiredPath.getValue().equals(path.getValue())).findFirst();
                if (singletonPathResult.isPresent()) {
                    PathElement singletonPath = singletonPathResult.get();
                    if (context.readResourceFromRoot(parentAddress, false).hasChild(singletonPath)) {
                        // If there is a required singleton sibling resource, we need to remove it first
                        PathAddress singletonAddress = parentAddress.append(singletonPath);
                        context.addStep(Util.createRemoveOperation(singletonAddress), context.getRootResourceRegistration().getOperationHandler(singletonAddress, ModelDescriptionConstants.REMOVE), OperationContext.Stage.MODEL);
                        context.addStep(operation, this, OperationContext.Stage.MODEL);
                        return;
                    }
                }
            }
        }

        super.execute(context, operation);
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        for (AttributeDefinition definition : this.descriptor.getExtraParameters()) {
            definition.validateOperation(operation);
        }
        super.populateModel(context, operation, resource);

        // Auto-create required child resources as necessary
        addRequiredChildren(context, this.descriptor.getRequiredChildren(), (Resource parent, PathElement path) -> parent.hasChild(path));
        addRequiredChildren(context, this.descriptor.getRequiredSingletonChildren(), (Resource parent, PathElement path) -> parent.hasChildren(path.getKey()));
    }

    private static void addRequiredChildren(OperationContext context, Collection<PathElement> paths, BiPredicate<Resource, PathElement> present) {
        paths.forEach(path -> context.addStep(Util.createAddOperation(context.getCurrentAddress().append(path)), new AddIfAbsentStepHandler(present), OperationContext.Stage.MODEL));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (this.handler != null) {
            this.handler.installServices(context, model);
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        if (this.handler != null) {
            try {
                this.handler.removeServices(context, resource.getModel());
            } catch (OperationFailedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        // The super implementation assumes that the capability name is a simple extension of the base name - we do not.
        this.descriptor.getCapabilities().forEach(capability -> context.registerCapability(capability.resolve(address)));

        ModelNode model = resource.getModel();
        this.attributes.stream()
                .filter(attribute -> attribute.hasCapabilityRequirements())
                .forEach(attribute -> attribute.addCapabilityRequirements(context, model.get(attribute.getName())));
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.descriptor.getDescriptionResolver()).withFlag(OperationEntry.Flag.RESTART_NONE);
        if (registration.isOrderedChildResource()) {
            builder.addParameter(SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ADD_INDEX, ModelType.INT, true).build());
        }
        Stream.concat(this.descriptor.getAttributes().stream(), this.descriptor.getExtraParameters().stream()).forEach(attribute -> builder.addParameter(attribute));
        registration.registerOperationHandler(builder.build(), this);

        this.descriptor.getAttributes().forEach(attribute -> registration.registerReadWriteAttribute(attribute, null, this.writeAttributeHandler));

        new CapabilityRegistration(this.descriptor.getCapabilities()).register(registration);
    }
}

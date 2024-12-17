/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Map;
import java.util.function.Predicate;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Generic remove operation step handler that delegates service removal/recovery to a dedicated {@link ResourceServiceHandler}.
 * @author Paul Ferraro
 */
public class RemoveStepHandler extends AbstractRemoveStepHandler implements ManagementRegistrar<ManagementResourceRegistration> {

    private final RemoveStepHandlerDescriptor descriptor;
    private final ResourceServiceHandler handler;
    private final OperationEntry.Flag flag;

    public RemoveStepHandler(RemoveStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        this(descriptor, handler, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    protected RemoveStepHandler(RemoveStepHandlerDescriptor descriptor, OperationEntry.Flag flag) {
        this(descriptor, null, flag);
    }

    private RemoveStepHandler(RemoveStepHandlerDescriptor descriptor, ResourceServiceHandler handler, OperationEntry.Flag flag) {
        this.descriptor = descriptor;
        this.handler = handler;
        this.flag = flag;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return super.requiresRuntime(context) && (this.handler != null);
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        if (removeInCurrentStep(resource)) {
            // We need to remove capabilities *before* removing the resource, since the capability reference resolution might involve reading the resource
            PathAddress address = context.getCurrentAddress();
            for (Map.Entry<RuntimeCapability<?>, Predicate<ModelNode>> entry : this.descriptor.getCapabilities().entrySet()) {
                if (entry.getValue().test(model)) {
                    RuntimeCapability<?> capability = entry.getKey();
                    context.deregisterCapability(capability.isDynamicallyNamed() ? capability.getDynamicName(address) : capability.getName());
                }
            }

            ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
            for (String attributeName : registration.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
                AttributeDefinition attribute = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName).getAttributeDefinition();
                if (attribute.hasCapabilityRequirements()) {
                    attribute.removeCapabilityRequirements(context, resource, model.get(attributeName));
                }
            }

            for (CapabilityReferenceRecorder recorder : registration.getRequirements()) {
                recorder.removeCapabilityRequirements(context, resource, null);
            }

            if (this.requiresRuntime(context)) {
                for (RuntimeResourceRegistration runtimeRegistration : this.descriptor.getRuntimeResourceRegistrations()) {
                    runtimeRegistration.unregister(context);
                }
            }
        }

        super.performRemove(context, operation, model);
    }

    /*
     * Determines whether resource removal happens in this step, or a subsequent step
     */
    private static boolean removeInCurrentStep(Resource resource) {
        for (String childType : resource.getChildTypes()) {
            for (Resource.ResourceEntry entry : resource.getChildren(childType)) {
                if (!entry.isRuntime() && resource.hasChild(entry.getPathElement())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            this.handler.removeServices(context, model);
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            this.handler.installServices(context, model);
        } else {
            context.revertReloadRequired();
        }
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        // We already unregistered our capabilities in performRemove(...)
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, this.descriptor.getDescriptionResolver()).withFlag(this.flag).build(), this.descriptor.getOperationTransformation().apply(this));
    }
}

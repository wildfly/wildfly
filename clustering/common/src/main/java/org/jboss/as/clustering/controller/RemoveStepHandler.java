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

import java.util.Map;
import java.util.function.Predicate;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
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
public class RemoveStepHandler extends AbstractRemoveStepHandler implements Registration<ManagementResourceRegistration> {

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
            for (Map.Entry<Capability, Predicate<ModelNode>> entry : this.descriptor.getCapabilities().entrySet()) {
                if (entry.getValue().test(model)) {
                    context.deregisterCapability(entry.getKey().resolve(address).getName());
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

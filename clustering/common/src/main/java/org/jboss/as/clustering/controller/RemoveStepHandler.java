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
import java.util.Objects;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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
        // Determine whether super impl will actually remove the resource
        boolean remove = !resource.getChildTypes().stream().anyMatch(type -> resource.getChildren(type).stream().filter(entry -> !entry.isRuntime()).map(entry -> entry.getPathElement()).anyMatch(path -> resource.hasChild(path)));
        if (remove) {
            // We need to remove capabilities *before* removing the resource, since the capability reference resolution might involve reading the resource
            PathAddress address = context.getCurrentAddress();
            this.descriptor.getCapabilities().entrySet().stream().filter(entry -> entry.getValue().test(model)).map(Map.Entry::getKey).forEach(capability -> context.deregisterCapability(capability.resolve(address).getName()));

            ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
            registration.getAttributeNames(PathAddress.EMPTY_ADDRESS).stream().map(name -> registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, name))
                    .filter(Objects::nonNull)
                    .map(access -> access.getAttributeDefinition())
                        .filter(Objects::nonNull)
                        .filter(attribute -> attribute.hasCapabilityRequirements())
                        .forEach(attribute -> attribute.removeCapabilityRequirements(context, model.get(attribute.getName())));

            // Remove any runtime child resources
            removeRuntimeChildren(context, PathAddress.EMPTY_ADDRESS);
        }

        super.performRemove(context, operation, model);

        if (remove) {
            PathAddress address = context.getResourceRegistration().getPathAddress();
            PathElement path = address.getLastElement();
            // If override model was registered, unregister it
            if (!path.isWildcard() && (context.getResourceRegistration().getParent().getSubModel(PathAddress.pathAddress(path.getKey(), PathElement.WILDCARD_VALUE)) != null)) {
                context.getResourceRegistrationForUpdate().unregisterOverrideModel(context.getCurrentAddressValue());
            }
        }
    }

    private static void removeRuntimeChildren(OperationContext context, PathAddress address) {
        Resource resource = context.readResource(address);
        for (String type : resource.getChildTypes()) {
            for (Resource.ResourceEntry entry : resource.getChildren(type)) {
                if (entry.isRuntime()) {
                    removeRuntimeChildren(context, address.append(entry.getPathElement()));
                    context.removeResource(address);
                }
            }
        }
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
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, this.descriptor.getDescriptionResolver()).withFlag(this.flag).build(), this);
    }
}

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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * {@link RestartParentResourceAddHandler} that leverages a {@link ResourceServiceBuilderFactory} for service recreation.
 * @author Paul Ferraro
 */
public class RestartParentAddHandler<T> extends RestartParentResourceAddHandler implements Registration {

    private final AddStepHandlerDescriptor descriptor;
    private final ResourceServiceBuilderFactory<T> builderFactory;

    public RestartParentAddHandler(AddStepHandlerDescriptor descriptor, ResourceServiceBuilderFactory<T> builderFactory) {
        super(null);
        this.descriptor = descriptor;
        this.builderFactory = builderFactory;
    }

    @Override
    protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
        super.updateModel(context, operation);
        PathAddress address = context.getCurrentAddress();
        for (Capability capability : this.descriptor.getCapabilities()) {
            context.registerCapability(capability.getRuntimeCapability(address), null);
        }
        // Copied from AbstractAddStepHandler.recordCapabilitiesAndRequirements(...)
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
            if (model.hasDefined(attribute.getName()) || attribute.hasCapabilityRequirements()) {
                attribute.addCapabilityRequirements(context, model.get(attribute.getName()));
            }
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        this.builderFactory.createBuilder(parentAddress).configure(context, parentModel).build(context.getServiceTarget()).install();
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return this.builderFactory.createBuilder(parentAddress).getServiceName();
    }

    @Override
    protected PathAddress getParentAddress(PathAddress address) {
        return address.getParent();
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.descriptor.getDescriptionResolver()).withFlag(OperationEntry.Flag.RESTART_NONE);
        for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
            builder.addParameter(attribute);
        }
        for (AttributeDefinition parameter : this.descriptor.getExtraParameters()) {
            builder.addParameter(parameter);
        }
        registration.registerOperationHandler(builder.build(), this);
    }
}

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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Generic add operation step handler that delegates service installation/rollback to a {@link ResourceServiceHandler}.
 * @author Paul Ferraro
 */
public class AddStepHandler extends AbstractAddStepHandler implements Registration {

    private final AddStepHandlerDescriptor descriptor;
    private final ResourceServiceHandler handler;

    public AddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor.getAttributes());
        this.descriptor = descriptor;
        this.handler = handler;
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

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        // The super implementation assumes that the capability name is a simple extension of the base name - we do not.
        for (Capability capability : this.descriptor.getCapabilities()) {
            context.registerCapability(capability.getRuntimeCapability(address), null);
        }
        super.recordCapabilitiesAndRequirements(context, operation, resource);
    }
}

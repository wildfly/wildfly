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
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Generic boot-time add step handler that delegates service installation/rollback to a {@link ResourceServiceHandler}.
 * @author Paul Ferraro
 */
public class BoottimeAddStepHandler extends AbstractBoottimeAddStepHandler implements Registration<ManagementResourceRegistration>, DescribedAddStepHandler {

    private final AddStepHandlerDescriptor descriptor;
    private final ResourceServiceHandler handler;

    public BoottimeAddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor.getAttributes());
        this.descriptor = descriptor;
        this.handler = handler;
    }

    @Override
    public AddStepHandlerDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        for (AttributeDefinition definition : this.descriptor.getExtraParameters()) {
            definition.validateOperation(operation);
        }
        super.populateModel(context, operation, resource);

        // Auto-create required child resources as necessary
        addRequiredChildren(context, this.descriptor.getRequiredChildren(), (Resource r, PathElement path) -> r.hasChild(path));
        addRequiredChildren(context, this.descriptor.getRequiredSingletonChildren(), (Resource r, PathElement path) -> r.hasChildren(path.getKey()));
    }

    private static void addRequiredChildren(OperationContext context, Collection<PathElement> paths, BiPredicate<Resource, PathElement> present) {
        for (PathElement path : paths) {
            context.addStep(Util.createAddOperation(context.getCurrentAddress().append(path)), new AddIfAbsentStepHandler(present), OperationContext.Stage.MODEL);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        this.handler.installServices(context, resource.getModel());
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
        // The super implementation assumes that the capability name is a simple extension of the base name - we do not.
        this.descriptor.getCapabilities().forEach(capability -> context.registerCapability(capability.resolve(address)));

        ModelNode model = resource.getModel();
        this.attributes.stream()
                .filter(attribute -> model.hasDefined(attribute.getName()) || attribute.hasCapabilityRequirements())
                .forEach(attribute -> attribute.addCapabilityRequirements(context, model.get(attribute.getName())));
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.descriptor.getDescriptionResolver()).withFlag(OperationEntry.Flag.RESTART_NONE);
        Stream.concat(this.descriptor.getAttributes().stream(), this.descriptor.getExtraParameters().stream()).forEach(attribute -> builder.addParameter(attribute));
        registration.registerOperationHandler(builder.build(), this);

        OperationStepHandler writeAttributeHandler = new ReloadRequiredWriteAttributeHandler(this.descriptor.getAttributes());
        this.descriptor.getAttributes().forEach(attribute -> registration.registerReadWriteAttribute(attribute, null, writeAttributeHandler));

        new CapabilityRegistration(this.descriptor.getCapabilities()).register(registration);
    }
}

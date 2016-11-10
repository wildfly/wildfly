/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Registers add, remove, and write-attribute operation handlers and capabilities.
 * @author Paul Ferraro
 */
public class ResourceRegistration implements Registration<ManagementResourceRegistration> {

    private final AddStepHandlerDescriptor descriptor;
    private final Registration<ManagementResourceRegistration> addRegistration;
    private final Registration<ManagementResourceRegistration> removeRegistration;
    private final Registration<ManagementResourceRegistration> writeAttributeRegistration;

    protected ResourceRegistration(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler, Registration<ManagementResourceRegistration> addRegistration, Registration<ManagementResourceRegistration> removeRegistration) {
        this(descriptor, addRegistration, removeRegistration, (handler != null) ? new ReloadRequiredWriteAttributeHandler(descriptor) : new ModelOnlyWriteAttributeHandler(descriptor));
    }

    protected ResourceRegistration(AddStepHandlerDescriptor descriptor, Registration<ManagementResourceRegistration> addRegistration, Registration<ManagementResourceRegistration> removeRegistration, Registration<ManagementResourceRegistration> writeAttributeRegistration) {
        this.descriptor = descriptor;
        this.addRegistration = addRegistration;
        this.removeRegistration = removeRegistration;
        this.writeAttributeRegistration = writeAttributeRegistration;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        this.addRegistration.register(registration);
        this.removeRegistration.register(registration);
        this.writeAttributeRegistration.register(registration);

        // Register read/write handlers for attribute aliases
        for (Map.Entry<AttributeDefinition, Attribute> entry : this.descriptor.getAttributeAliases().entrySet()) {
            Attribute target = entry.getValue();
            String targetName = target.getDefinition().getName();
            AttributeAccess targetAccess = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, targetName);
            // If target attribute has no read handler, synthesize one
            OperationStepHandler readHandler = (targetAccess.getReadHandler() != null) ? targetAccess.getReadHandler() : (context, operation) -> {
                ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                ModelNode result = context.getResult();
                if (model.hasDefined(targetName)) {
                    result.set(model.get(targetName));
                } else if (Operations.isIncludeDefaults(operation)) {
                    result.set(target.getDefinition().getDefaultValue());
                }
            };
            OperationStepHandler writeHandler = targetAccess.getWriteHandler();
            // Delegate read/write attribute operations to target attribute
            registration.registerReadWriteAttribute(entry.getKey(),
                    (context, operation) -> context.addStep(Operations.createReadAttributeOperation(context.getCurrentAddress(), target), readHandler, OperationContext.Stage.MODEL),
                    (context, operation) -> context.addStep(Operations.createWriteAttributeOperation(context.getCurrentAddress(), target, Operations.getAttributeValue(operation)), writeHandler, OperationContext.Stage.MODEL));
        }

        new CapabilityRegistration(this.descriptor.getCapabilities().keySet()).register(registration);
    }
}

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
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.operations.global.ListOperations;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.operations.global.WriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

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
        this(descriptor, addRegistration, removeRegistration, new WriteAttributeStepHandler(descriptor, handler));
    }

    protected ResourceRegistration(AddStepHandlerDescriptor descriptor, Registration<ManagementResourceRegistration> addRegistration, Registration<ManagementResourceRegistration> removeRegistration, Registration<ManagementResourceRegistration> writeAttributeRegistration) {
        this.descriptor = descriptor;
        this.addRegistration = addRegistration;
        this.removeRegistration = removeRegistration;
        this.writeAttributeRegistration = writeAttributeRegistration;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new CapabilityRegistration(this.descriptor.getCapabilities().keySet()).register(registration);

        registration.registerRequirements(this.descriptor.getResourceCapabilityReferences());

        // Register attributes before add operation
        this.writeAttributeRegistration.register(registration);

        // Register attribute translations
        for (Map.Entry<AttributeDefinition, AttributeTranslation> entry : this.descriptor.getAttributeTranslations().entrySet()) {
            AttributeTranslation translation = entry.getValue();
            registration.registerReadWriteAttribute(entry.getKey(), new ReadAttributeTranslationHandler(translation), new WriteAttributeTranslationHandler(translation));
        }

        this.addRegistration.register(registration);
        this.removeRegistration.register(registration);

        // Override global operations with transformed operations, if necessary
        this.registerTransformedOperation(registration, WriteAttributeHandler.DEFINITION, WriteAttributeHandler.INSTANCE);

        this.registerTransformedOperation(registration, MapOperations.MAP_PUT_DEFINITION, MapOperations.MAP_PUT_HANDLER);
        this.registerTransformedOperation(registration, MapOperations.MAP_GET_DEFINITION, MapOperations.MAP_GET_HANDLER);
        this.registerTransformedOperation(registration, MapOperations.MAP_REMOVE_DEFINITION, MapOperations.MAP_REMOVE_HANDLER);
        this.registerTransformedOperation(registration, MapOperations.MAP_CLEAR_DEFINITION, MapOperations.MAP_CLEAR_HANDLER);

        this.registerTransformedOperation(registration, ListOperations.LIST_ADD_DEFINITION, ListOperations.LIST_ADD_HANDLER);
        this.registerTransformedOperation(registration, ListOperations.LIST_GET_DEFINITION, ListOperations.LIST_GET_HANDLER);
        this.registerTransformedOperation(registration, ListOperations.LIST_REMOVE_DEFINITION, ListOperations.LIST_REMOVE_HANDLER);
        this.registerTransformedOperation(registration, ListOperations.LIST_CLEAR_DEFINITION, ListOperations.LIST_CLEAR_HANDLER);
    }

    private void registerTransformedOperation(ManagementResourceRegistration registration, OperationDefinition definition, OperationStepHandler handler) {
        // Only override global operation handlers for non-identity transformations
        OperationStepHandler transformedHandler = this.descriptor.getOperationTransformation().apply(handler);
        if (handler != transformedHandler) {
            registration.registerOperationHandler(definition, transformedHandler);
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
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
public class ResourceRegistrar implements ManagementRegistrar<ManagementResourceRegistration> {

    private final AddStepHandlerDescriptor descriptor;
    private final ManagementRegistrar<ManagementResourceRegistration> addRegistration;
    private final ManagementRegistrar<ManagementResourceRegistration> removeRegistration;
    private final ManagementRegistrar<ManagementResourceRegistration> writeAttributeRegistration;

    protected ResourceRegistrar(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler, ManagementRegistrar<ManagementResourceRegistration> addRegistration, ManagementRegistrar<ManagementResourceRegistration> removeRegistration) {
        this(descriptor, addRegistration, removeRegistration, new WriteAttributeStepHandler(descriptor, handler));
    }

    protected ResourceRegistrar(AddStepHandlerDescriptor descriptor, ManagementRegistrar<ManagementResourceRegistration> addRegistration, ManagementRegistrar<ManagementResourceRegistration> removeRegistration, ManagementRegistrar<ManagementResourceRegistration> writeAttributeRegistration) {
        this.descriptor = descriptor;
        this.addRegistration = addRegistration;
        this.removeRegistration = removeRegistration;
        this.writeAttributeRegistration = writeAttributeRegistration;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        this.descriptor.getCapabilities().keySet().forEach(registration::registerCapability);

        registration.registerRequirements(this.descriptor.getResourceCapabilityReferences());

        // Register standard attributes before add operation
        this.writeAttributeRegistration.register(registration);

        // Register attributes with custom write-attribute handlers
        for (Map.Entry<AttributeDefinition, OperationStepHandler> entry : this.descriptor.getCustomAttributes().entrySet()) {
            registration.registerReadWriteAttribute(entry.getKey(), null, entry.getValue());
        }

        // Register attributes that will be ignored at runtime
        Collection<AttributeDefinition> ignoredAttributes = this.descriptor.getIgnoredAttributes();
        if (!ignoredAttributes.isEmpty()) {
            for (AttributeDefinition ignoredAttribute : ignoredAttributes) {
                registration.registerReadWriteAttribute(ignoredAttribute, null, ModelOnlyWriteAttributeHandler.INSTANCE);
            }
        }

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

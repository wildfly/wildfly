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

import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.operations.global.ListOperations;
import org.jboss.as.controller.operations.global.MapOperations;
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

        // Register attributes before add operation
        this.writeAttributeRegistration.register(registration);

        // Register attribute translations
        this.descriptor.getAttributeTranslations().entrySet().forEach(entry -> registration.registerReadWriteAttribute(entry.getKey(), new ReadAttributeTranslationHandler(entry.getValue()), new WriteAttributeTranslationHandler(entry.getValue())));

        this.addRegistration.register(registration);
        this.removeRegistration.register(registration);

        UnaryOperator<OperationStepHandler> transformer = this.descriptor.getOperationTransformation();

        registration.registerOperationHandler(MapOperations.MAP_PUT_DEFINITION, transformer.apply(MapOperations.MAP_PUT_HANDLER));
        registration.registerOperationHandler(MapOperations.MAP_GET_DEFINITION, transformer.apply(MapOperations.MAP_GET_HANDLER));
        registration.registerOperationHandler(MapOperations.MAP_REMOVE_DEFINITION, transformer.apply(MapOperations.MAP_REMOVE_HANDLER));
        registration.registerOperationHandler(MapOperations.MAP_CLEAR_DEFINITION, transformer.apply(MapOperations.MAP_CLEAR_HANDLER));

        registration.registerOperationHandler(ListOperations.LIST_ADD_DEFINITION, transformer.apply(ListOperations.LIST_ADD_HANDLER));
        registration.registerOperationHandler(ListOperations.LIST_REMOVE_DEFINITION, transformer.apply(ListOperations.LIST_REMOVE_HANDLER));
        registration.registerOperationHandler(ListOperations.LIST_GET_DEFINITION, transformer.apply(ListOperations.LIST_GET_HANDLER));
        registration.registerOperationHandler(ListOperations.LIST_CLEAR_DEFINITION, transformer.apply(ListOperations.LIST_CLEAR_HANDLER));
    }
}

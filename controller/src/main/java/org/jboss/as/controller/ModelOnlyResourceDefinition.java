/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * {@link ResourceDefinition} implementation designed for use in extensions
 * based on {@link org.jboss.as.controller.extension.AbstractLegacyExtension}. Takes the {@link AttributeDefinition}s provided to the constructor
 * and uses them to create a {@link ModelOnlyAddStepHandler} for handling {@code add} operations, and to
 * create a {@link ModelOnlyWriteAttributeHandler} for handling {@code write-attribute} operations. The
 * {@link ModelOnlyRemoveStepHandler} is used for {@code remove} operations.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ModelOnlyResourceDefinition extends SimpleResourceDefinition {

    private final AttributeDefinition[] attributes;

    public ModelOnlyResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, AttributeDefinition... attributes) {
        super(pathElement, descriptionResolver, new ModelOnlyAddStepHandler(attributes), ModelOnlyRemoveStepHandler.INSTANCE);
        this.attributes = attributes;
    }

    public ModelOnlyResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, ModelOnlyAddStepHandler addStepHandler, AttributeDefinition... attributes) {
        super(pathElement, descriptionResolver, addStepHandler, ModelOnlyRemoveStepHandler.INSTANCE);
        this.attributes = attributes;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ModelOnlyWriteAttributeHandler(attributes);
        for (AttributeDefinition ad : attributes) {
            resourceRegistration.registerReadWriteAttribute(ad, null, writeHandler);
        }
    }
}

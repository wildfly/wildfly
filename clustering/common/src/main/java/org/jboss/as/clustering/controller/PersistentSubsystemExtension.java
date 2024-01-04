/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Supplier;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemModel;

/**
 * Generic extension implementation that registers a single subsystem whose XML readers/writer are created from a {@link PersistentResourceXMLDescription}.
 * @author Paul Ferraro
 */
public class PersistentSubsystemExtension<S extends Enum<S> & PersistentSubsystemSchema<S>> extends SubsystemExtension<S> {

    /**
     * Constructs a new extension using a reader/writer factory.
     * @param name the subsystem name
     * @param currentModel the current model
     * @param registrarFactory a factory for creating the subsystem resource registrar
     * @param currentSchema the current schema
     * @param descriptionFactory an XML description factory
     */
    protected PersistentSubsystemExtension(String name, SubsystemModel currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema) {
        // Build xml description for current schema only once
        this(name, currentModel, registrarFactory, currentSchema, currentSchema.getXMLDescription());
    }

    private PersistentSubsystemExtension(String name, SubsystemModel currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, PersistentResourceXMLDescription currentDescription) {
        // Reuse current xml description between reader and writer
        super(name, currentModel, registrarFactory, currentSchema, new PersistentResourceXMLDescriptionWriter(currentDescription), new PersistentResourceXMLDescriptionReader(currentDescription));
    }
}

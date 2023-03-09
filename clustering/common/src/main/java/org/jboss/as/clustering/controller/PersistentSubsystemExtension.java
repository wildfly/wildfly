/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

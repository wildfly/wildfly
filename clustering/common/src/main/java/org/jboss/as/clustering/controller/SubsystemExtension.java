/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Generic extension implementation that registers a single subsystem.
 * @author Paul Ferraro
 */
public class SubsystemExtension<S extends Enum<S> & SubsystemSchema<S>> implements Extension {

    private final String name;
    private final SubsystemModel currentModel;
    private final Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory;
    private final S currentSchema;
    private final XMLElementWriter<SubsystemMarshallingContext> writer;
    private final XMLElementReader<List<ModelNode>> currentReader;

    /**
     * Constructs a new extension using a reader factory and a separate writer implementation.
     * @param name the subsystem name
     * @param currentModel the current model
     * @param registrarFactory a factory for creating the subsystem resource definition registrar
     * @param currentSchema the current schema
     * @param readerFactory a factory for creating an XML reader
     * @param writer an XML writer
     */
    protected SubsystemExtension(String name, SubsystemModel currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, XMLElementWriter<SubsystemMarshallingContext> writer) {
        this(name, currentModel, registrarFactory, currentSchema, writer, currentSchema);
    }

    SubsystemExtension(String name, SubsystemModel currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, XMLElementWriter<SubsystemMarshallingContext> writer, XMLElementReader<List<ModelNode>> currentReader) {
        this.name = name;
        this.currentModel = currentModel;
        this.registrarFactory = registrarFactory;
        this.currentSchema = currentSchema;
        this.writer = writer;
        this.currentReader = currentReader;
    }

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = new ContextualSubsystemRegistration(context.registerSubsystem(this.name, this.currentModel.getVersion()), context);
        // Construct subsystem resource definition here so that instance can be garbage collected following registration
        this.registrarFactory.get().register(registration);
        registration.registerXMLElementWriter(this.writer);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (S schema : EnumSet.allOf(this.currentSchema.getDeclaringClass())) {
            context.setSubsystemXmlMapping(this.name, schema.getNamespace().getUri(), (schema == this.currentSchema) ? this.currentReader: schema);
        }
    }
}

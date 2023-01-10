/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Generic extension implementation that registers a single subsystem.
 * @author Paul Ferraro
 */
public class SubsystemExtension<S extends Enum<S> & SubsystemSchema<S>> implements Extension {

    private final String name;
    private final Model currentModel;
    private final Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory;
    private final S currentSchema;
    private final Function<S, XMLElementReader<List<ModelNode>>> readerFactory;
    private final XMLElementWriter<SubsystemMarshallingContext> writer;
    private final XMLElementReader<List<ModelNode>> currentReader;

    /**
     * Constructs a new extension using a reader/writer factory.
     * @param name the subsystem name
     * @param currentModel the current model
     * @param registrarFactory a factory for creating the subsystem resource registrar
     * @param currentSchema the current schema
     * @param descriptionFactory an XML description factory
     */
    public SubsystemExtension(String name, Model currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, Function<S, PersistentResourceXMLDescription> descriptionFactory) {
        // Build xml description for current schema only once
        this(name, currentModel, registrarFactory, currentSchema, descriptionFactory, descriptionFactory.apply(currentSchema));
    }

    private SubsystemExtension(String name, Model currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, Function<S, PersistentResourceXMLDescription> descriptionFactory, PersistentResourceXMLDescription currentDescription) {
        // Reuse current xml description between reader and writer
        this(name, currentModel, registrarFactory, currentSchema, descriptionFactory.andThen(XMLDescriptionReader::new), new XMLDescriptionWriter(currentDescription), new XMLDescriptionReader(currentDescription));
    }

    /**
     * Constructs a new extension using a reader factory and a separate writer implementation.
     * @param name the subsystem name
     * @param currentModel the current model
     * @param registrarFactory a factory for creating the subsystem resource definition registrar
     * @param currentSchema the current schema
     * @param readerFactory a factory for creating an XML reader
     * @param writer an XML writer
     */
    public SubsystemExtension(String name, Model currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, Function<S, XMLElementReader<List<ModelNode>>> readerFactory, XMLElementWriter<SubsystemMarshallingContext> writer) {
        this(name, currentModel, registrarFactory, currentSchema, readerFactory, writer, readerFactory.apply(currentSchema));
    }

    private SubsystemExtension(String name, Model currentModel, Supplier<ManagementRegistrar<SubsystemRegistration>> registrarFactory, S currentSchema, Function<S, XMLElementReader<List<ModelNode>>> readerFactory, XMLElementWriter<SubsystemMarshallingContext> writer, XMLElementReader<List<ModelNode>> currentReader) {
        this.name = name;
        this.currentModel = currentModel;
        this.registrarFactory = registrarFactory;
        this.currentSchema = currentSchema;
        this.readerFactory = readerFactory;
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
        // Register reader for current schema version
        context.setSubsystemXmlMapping(this.name, this.currentSchema.getUri(), this.currentReader);
        // Register readers for legacy schema versions
        for (S schema : EnumSet.complementOf(EnumSet.of(this.currentSchema))) {
            context.setSubsystemXmlMapping(this.name, schema.getUri(), this.readerFactory.apply(schema));
        }
    }

    private static class XMLDescriptionReader implements XMLElementReader<List<ModelNode>> {
        private final PersistentResourceXMLDescription description;

        XMLDescriptionReader(PersistentResourceXMLDescription description) {
            this.description = description;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
            this.description.parse(reader, PathAddress.EMPTY_ADDRESS, operations);
        }
    }

    private static class XMLDescriptionWriter implements XMLElementWriter<SubsystemMarshallingContext> {
        private final PersistentResourceXMLDescription description;

        XMLDescriptionWriter(PersistentResourceXMLDescription description) {
            this.description = description;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            ModelNode model = new ModelNode();
            model.get(this.description.getPathElement().getKeyValuePair()).set(context.getModelNode());
            this.description.persist(writer, model);
        }
    }
}

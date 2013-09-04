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

package org.wildfly.extension.batch;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.batch._private.BatchLogger;

public class BatchSubsystemDefinition extends PersistentResourceDefinition {

    /**
     * The name of our subsystem within the model.
     */
    public static final String NAME = "batch";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, NAME);

    static final String DATASOURCE = "datasource";
    static final String IN_MEMORY = "in-memory";
    static final String JDBC = "jdbc";
    static final String JNDI_NAME = "jndi-name";
    private static final String RESOURCE_NAME = BatchSubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
    }

    public static final SimpleAttributeDefinition JOB_REPOSITORY =
            SimpleAttributeDefinitionBuilder.create("job-repository", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(IN_MEMORY))
                    .setAttributeMarshaller(new DefaultAttributeMarshaller() {
                        @Override
                        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
                            if (isMarshallable(attribute, resourceModel, marshallDefault)) {
                                writer.writeStartElement(attribute.getName());
                                if (resourceModel.hasDefined(attribute.getName())) {
                                    final String value = resourceModel.get(attribute.getName()).asString();
                                    if (IN_MEMORY.equals(value)) {
                                        writer.writeStartElement(value);
                                        writer.writeEndElement();
                                    } else {
                                        writer.writeStartElement(JDBC);
                                        writer.writeStartElement(DATASOURCE);
                                        writer.writeAttribute(JNDI_NAME, value);
                                        writer.writeEndElement();
                                        writer.writeEndElement();
                                    }
                                } else {
                                    writer.writeStartElement(IN_MEMORY);
                                    writer.writeEndElement();
                                }
                                writer.writeEndElement();
                            }
                        }
                    })
                    .setValidator(new StringLengthValidator(1, true, true))
                    .build();

   static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList((AttributeDefinition) JOB_REPOSITORY);

    public static final BatchSubsystemDefinition INSTANCE = new BatchSubsystemDefinition();

    private BatchSubsystemDefinition() {
        super(SUBSYSTEM_PATH, getResourceDescriptionResolver(null), BatchSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(JOB_REPOSITORY, null, new ReloadRequiredWriteAttributeHandler(JOB_REPOSITORY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    // TODO (jrp) use switch for string comparison
    static class BatchResourceXMLDescription extends PersistentResourceXMLDescription {

        protected BatchResourceXMLDescription(final PersistentResourceDefinition resourceDefinition, final String xmlElementName, final String xmlWrapperElement, final LinkedHashMap<String, AttributeDefinition> attributes, final List<PersistentResourceXMLDescription> children, final boolean useValueAsElementName, final boolean noAddOperation, final AdditionalOperationsGenerator additionalOperationsGenerator) {
            super(resourceDefinition, xmlElementName, xmlWrapperElement, attributes, children, useValueAsElementName, noAddOperation, additionalOperationsGenerator);
        }

        @Override
        public void parse(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final List<ModelNode> list) throws XMLStreamException {
            final ModelNode subsystemAdd = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM_PATH));

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
                if (namespace == Namespace.BATCH_1_0) {
                    final String localName = reader.getLocalName();
                    if (JOB_REPOSITORY.getXmlName().equals(localName)) {
                        parseJobRepository(reader, subsystemAdd);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
            ParseUtils.requireNoContent(reader);
            list.add(subsystemAdd);
        }

        private void parseJobRepository(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final String localName = reader.getLocalName();
                final String value;
                if (IN_MEMORY.equals(localName)) {
                    value = IN_MEMORY;
                } else if (JDBC.equals(localName)) {
                    if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        value = ParseUtils.readStringAttributeElement(reader, JNDI_NAME);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
                BatchSubsystemDefinition.JOB_REPOSITORY.parseAndSetParameter(value, operation, reader);
                BatchLogger.LOGGER.tracef("Configured batch job repository type is %s", value);
            }
        }

        @Override
        public void persist(final XMLExtendedStreamWriter writer, final ModelNode model, final String namespaceURI) throws XMLStreamException {
            JOB_REPOSITORY.marshallAsElement(model, true, writer);
        }
    }

    static class BatchResourceXMLBuilder extends PersistentResourceXMLBuilder {

        protected BatchResourceXMLBuilder(final PersistentResourceDefinition definition) {
            super(definition);
        }

        static BatchResourceXMLBuilder builder(final PersistentResourceDefinition definition) {
            return new BatchResourceXMLBuilder(definition);
        }

        @Override
        public PersistentResourceXMLDescription build() {
            return new BatchResourceXMLDescription(resourceDefinition, xmlElementName, xmlWrapperElement,
                    attributes, new ArrayList<PersistentResourceXMLDescription>(), useValueAsElementName, noAddOperation,
                    additionalOperationsGenerator);
        }
    }
}

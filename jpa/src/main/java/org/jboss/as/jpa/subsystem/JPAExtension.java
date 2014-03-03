/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jpa.messages.JpaLogger.JPA_LOGGER;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderLoader;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the JPA subsystem element handlers.
 *
 * @author Scott Marlow
 */
public class JPAExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jpa";

    private static final JPASubsystemElementParser1_1 parser1_1 = new JPASubsystemElementParser1_1();
    private static final JPASubsystemElementParser1_0 parser1_0 = new JPASubsystemElementParser1_0();

    private static final String RESOURCE_NAME = JPAExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JPAExtension.class.getClassLoader(), true, false);
    }
    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;


    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration nodeRegistration = registration.registerSubsystemModel(JPADefinition.INSTANCE);
        nodeRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        registration.registerXMLElementWriter(parser1_1);

        if (context.isRegisterTransformers()) {
            initializeTransformers_1_1_0(registration);
        }

        try {
            PersistenceProviderLoader.loadDefaultProvider();
        } catch (ModuleLoadException e) {
            JPA_LOGGER.errorPreloadingDefaultProvider(e);
        }

        if (context.isRuntimeOnlyRegistrationValid()) {
            final ManagementResourceRegistration jpaSubsystemDeployments = registration.registerDeploymentModel(JPADefinition.INSTANCE);

        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JPA_1_1.getUriString(), parser1_1);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JPA_1_0.getUriString(), parser1_0);
    }

    private void initializeTransformers_1_1_0(SubsystemRegistration subsystemRegistration) {

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, JPADefinition.DEFAULT_DATASOURCE)
            .addRejectCheck(RejectAttributeChecker.DEFINED, JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)
            .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(ExtendedPersistenceInheritance.DEEP.toString())), JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)
            .end();
        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 1, 0));
    }

    static class JPASubsystemElementParser1_1 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ModelNode subsystemAdd = null;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());

                switch (element) {
                    case JPA: {
                        subsystemAdd = parseJPA(reader, readerNS);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            if (subsystemAdd == null) {
                throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.JPA.getLocalName()));
            }
            list.add(subsystemAdd);
        }

        private ModelNode parseJPA(XMLExtendedStreamReader reader, Namespace readerNS) throws XMLStreamException {
            String dataSourceName = null;
            final ModelNode operation = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME)));

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEFAULT_DATASOURCE_NAME: {
                        dataSourceName = value;
                        JPADefinition.DEFAULT_DATASOURCE.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                    case DEFAULT_EXTENDEDPERSISTENCEINHERITANCE_NAME:
                        JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE.parseAndSetParameter(value, operation, reader);
                        break;
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if (dataSourceName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.DEFAULT_DATASOURCE_NAME));
            }
            return operation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws
            XMLStreamException {

            ModelNode node = context.getModelNode();
            if (node.hasDefined(CommonAttributes.DEFAULT_DATASOURCE) ||
                    node.hasDefined(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)
                    ) {
                context.startSubsystemElement(Namespace.JPA_1_1.getUriString(), false);
                writer.writeStartElement(Element.JPA.getLocalName());
                JPADefinition.DEFAULT_DATASOURCE.marshallAsAttribute(node, writer);
                JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE.marshallAsAttribute(node, writer);
                writer.writeEndElement();
                writer.writeEndElement();
            } else {
                //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
                //context.startSubsystemElement(NewNamingExtension.NAMESPACE, true);
                context.startSubsystemElement(Namespace.JPA_1_1.getUriString(), false);
                writer.writeEndElement();
            }

        }
    }

    static class JPASubsystemElementParser1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ModelNode subsystemAdd = null;

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());

                switch (element) {
                    case JPA: {
                        subsystemAdd = parseJPA(reader, readerNS);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            if (subsystemAdd == null) {
                throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.JPA.getLocalName()));
            }
            list.add(subsystemAdd);
        }

        private ModelNode parseJPA(XMLExtendedStreamReader reader, Namespace readerNS) throws XMLStreamException {
            final ModelNode operation = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME)));
            String dataSourceName = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEFAULT_DATASOURCE_NAME: {
                        dataSourceName = value;
                        JPADefinition.DEFAULT_DATASOURCE.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if (dataSourceName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.DEFAULT_DATASOURCE_NAME));
            }
            return operation;
        }
    }
}

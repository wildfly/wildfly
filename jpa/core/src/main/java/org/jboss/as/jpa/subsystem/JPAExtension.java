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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderLoader;
import org.jboss.as.jpa.processor.PersistenceProviderAdaptorLoader;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
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

    private static final JPASubsystemElementParser parser = new JPASubsystemElementParser();

    private static final DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JPADescriptions.getSubsystemDescription(locale);
        }
    };

    private static ModelNode createAddOperation(String defaultDatasource) {
        final ModelNode update = new ModelNode();
        update.get(OP).set(ADD);
        update.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        update.get(CommonAttributes.DEFAULT_DATASOURCE).set(defaultDatasource);
        return update;
    }


    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration nodeRegistration = registration.registerSubsystemModel(DESCRIPTION);
        PersistenceUnitRegistryImpl persistenceUnitRegistry = new PersistenceUnitRegistryImpl();
        JPASubSystemAdd subsystemAdd = new JPASubSystemAdd(persistenceUnitRegistry);
        nodeRegistration.registerOperationHandler(JPASubSystemAdd.OPERATION_NAME, subsystemAdd, subsystemAdd, false);
        nodeRegistration.registerOperationHandler(JPASubSystemRemove.OPERATION_NAME, JPASubSystemRemove.INSTANCE, JPASubSystemRemove.INSTANCE, false);
        nodeRegistration.registerOperationHandler(DESCRIBE, JPADescribeHandler.INSTANCE, JPADescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        nodeRegistration.registerReadWriteAttribute(CommonAttributes.DEFAULT_DATASOURCE, null, JPADefaultDatasourceWriteHandler.INSTANCE, Storage.CONFIGURATION);
        registration.registerXMLElementWriter(parser);

        try {
            PersistenceProviderLoader.loadDefaultProvider();
        } catch (ModuleLoadException e) {
            JPA_LOGGER.errorPreloadingDefaultProvider(e);
        }

        try {
            // load the default persistence provider adaptor
            PersistenceProviderAdaptor provider = PersistenceProviderAdaptorLoader.loadPersistenceAdapterModule(Configuration.ADAPTER_MODULE_DEFAULT);
            final ManagementAdaptor managementAdaptor = provider.getManagementAdaptor();
            if (managementAdaptor != null) {
                DescriptionProvider JPA_SUBSYSTEM = new DescriptionProvider() {
                    @Override
                    public ModelNode getModelDescription(Locale locale) {
                        ModelNode subsystem = new ModelNode();
                        subsystem.get(org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION).set("Runtime information about JPA use in the deployment.");
                        subsystem.get(ATTRIBUTES).setEmptyObject();
                        subsystem.get("operations"); // placeholder

                        subsystem.get(CHILDREN, managementAdaptor.getIdentificationLabel(),
                            org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION).
                            set("Runtime information about " + managementAdaptor.getIdentificationLabel() + " use in the deployment.");

                        subsystem.get(CHILDREN, managementAdaptor.getIdentificationLabel(), MIN_OCCURS).set(0);
                        return subsystem;
                    }
                };
                final ManagementResourceRegistration jpaSubsystemDeployments = registration.registerDeploymentModel(JPA_SUBSYSTEM);

                managementAdaptor.register(jpaSubsystemDeployments, persistenceUnitRegistry);
            }
        } catch (ModuleLoadException e) {
            JPA_LOGGER.errorPreloadingDefaultProviderAdaptor(e);
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), parser);
    }

    private static class JPADescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final JPADescribeHandler INSTANCE = new JPADescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);
            context.getResult().add(createAddOperation(model.require(CommonAttributes.DEFAULT_DATASOURCE).asString()));
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

    static class JPASubsystemElementParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ModelNode subsystemAdd = null;

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case JPA: {
                        subsystemAdd = parseJPA(reader);
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

        private ModelNode parseJPA(XMLExtendedStreamReader reader) throws XMLStreamException {
            String dataSourceName = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEFAULT_DATASOURCE_NAME: {
                        dataSourceName = value;
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
            return createAddOperation(dataSourceName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws
            XMLStreamException {

            ModelNode node = context.getModelNode();
            if (node.has(CommonAttributes.DEFAULT_DATASOURCE)) {
                context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
                writer.writeStartElement(Element.JPA.getLocalName());
                writer.writeAttribute(Attribute.DEFAULT_DATASOURCE_NAME.getLocalName(), node.get(CommonAttributes.DEFAULT_DATASOURCE).asString());
                writer.writeEndElement();
                writer.writeEndElement();
            } else {
                //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
                //context.startSubsystemElement(NewNamingExtension.NAMESPACE, true);
                context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
                writer.writeEndElement();
            }

        }
    }

}

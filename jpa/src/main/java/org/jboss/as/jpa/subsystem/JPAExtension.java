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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

/**
 * Domain extension used to initialize the JPA subsystem element handlers.
 *
 * @author Scott Marlow
 */
public class JPAExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jpa";

    static final String RESOURCE_NAME = JPAExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final JPASubsystemElementParser parser = new JPASubsystemElementParser();

    private static final DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JPASubsystemProviders.SUBSYSTEM.getModelDescription(locale);
        }
    };

    private static final DescriptionProvider JPA_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(JPADataSourceAdd.OPERATION_NAME);
            op.get(org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("jpa.add"));

            op.get(REQUEST_PROPERTIES,
                CommonAttributes.DEFAULT_DATASOURCE,
                org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("default.datasource"));
            op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, MIN_OCCURS).set(1);
            op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, MAX_OCCURS).set(1);

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }
    };

    private static final DescriptionProvider JPA_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(JPADataSourceRemove.OPERATION_NAME);
            op.get(org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("jpa.remove"));

            op.get(REQUEST_PROPERTIES).setEmptyObject();

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

    };


    private static ModelNode createAddOperation() {
        final ModelNode update = new ModelNode();
        update.get(OP).set(ADD);
        update.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        return update;
    }

    private static ModelNode createAddDefaultDataSourceName(String dataSourceName) {
        final ModelNode jpaModelNode = new ModelNode();
        jpaModelNode.get(OP).set(JPADataSourceAdd.OPERATION_NAME);
        jpaModelNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        jpaModelNode.get(CommonAttributes.DEFAULT_DATASOURCE).set(dataSourceName);
        return jpaModelNode;
    }


    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration nodeRegistration = registration.registerSubsystemModel(DESCRIPTION);
        nodeRegistration.registerOperationHandler(ADD, JPASubSystemAdd.INSTANCE, DESCRIPTION, false);
        nodeRegistration.registerOperationHandler(DESCRIBE, JPADescribeHandler.INSTANCE, JPADescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        nodeRegistration.registerOperationHandler(JPADataSourceAdd.OPERATION_NAME, JPADataSourceAdd.INSTANCE, JPA_ADD, false);
        nodeRegistration.registerOperationHandler(JPADataSourceRemove.OPERATION_NAME, JPADataSourceRemove.INSTANCE, JPA_REMOVE, false);
        registration.registerXMLElementWriter(parser);

    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }

    private static class JPADescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final JPADescribeHandler INSTANCE = new JPADescribeHandler();

        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
            ModelNode node = new ModelNode();
            node.add(createAddOperation());

            resultHandler.handleResultFragment(Util.NO_LOCATION, node);
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
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
            list.add(createAddOperation());

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case JPA: {
                        parseJPA(reader, list);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }

        }

        private void parseJPA(XMLExtendedStreamReader reader, List<ModelNode> list) throws
            XMLStreamException {
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
            list.add(createAddDefaultDataSourceName(dataSourceName));
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

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The web services transactions extension.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
public class XTSExtension implements Extension {
    private static final Logger log = Logger.getLogger("org.jboss.as.xts");

    public static final String SUBSYSTEM_NAME = "xts";
    private static final XTSSubsystemParser parser = new XTSSubsystemParser();


    public void initialize(ExtensionContext context) {
        log.debug("Initializing XTS Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(XTSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, XTSSubsystemAdd.INSTANCE, XTSSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, XTSSubsystemRemove.INSTANCE, XTSSubsystemProviders.SUBSYSTEM_REMOVE, false);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(parser);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }

    private static ModelNode createEmptyAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        subsystem.get(ModelDescriptionConstants.OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    static class XTSSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }

            final ModelNode subsystem = createEmptyAddOperation();
            list.add(subsystem);


            // elements
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case XTS_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (! encountered.add(element)) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        switch (element) {
                            case XTS_ENVIRONMENT: {
                                final ModelNode model = parseXTSEnvironmentElement(reader);
                                subsystem.get(CommonAttributes.XTS_ENVIRONMENT).set(model) ;
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        /**
         * Handle the xts-environment element
         * @param reader
         * @return ModelNode for the core-environment
         * @throws XMLStreamException
         */
        static ModelNode parseXTSEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode store = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                ParseUtils.requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case URL:
                        store.get(ModelDescriptionConstants.URL).set(value);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
            // Handle elements
            ParseUtils.requireNoContent(reader);
            return store;
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

            ModelNode node = context.getModelNode();

            if (has(node, CommonAttributes.XTS_ENVIRONMENT)) {
                writer.writeStartElement(Element.XTS_ENVIRONMENT.getLocalName());
                final ModelNode xtsEnv = node.get(CommonAttributes.XTS_ENVIRONMENT);
                if (has(xtsEnv, ModelDescriptionConstants.URL)) {
                    writeAttribute(writer, Attribute.URL, xtsEnv.get(ModelDescriptionConstants.URL));
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }
    }

}

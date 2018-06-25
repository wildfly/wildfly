/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.xts.XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION;
import static org.jboss.as.xts.XTSSubsystemDefinition.ASYNC_REGISTRATION;
import static org.jboss.as.xts.XTSSubsystemDefinition.ENVIRONMENT_URL;
import static org.jboss.as.xts.XTSSubsystemDefinition.HOST_NAME;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class XTSSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }

        final ModelNode subsystem = Util.getEmptyOperation(ADD, PathAddress.pathAddress(XTSExtension.SUBSYSTEM_PATH)
                .toModelNode());
        list.add(subsystem);

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final List<Element> expected = getExpectedElements(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());

            if (!expected.contains(element) || !encountered.add(element)) {
                throw ParseUtils.unexpectedElement(reader);
            }

            switch (element) {
                case HOST: {
                    parseHostElement(reader, subsystem);
                    break;
                }
                case XTS_ENVIRONMENT: {
                    parseXTSEnvironmentElement(reader,subsystem);
                    break;
                }
                case DEFAULT_CONTEXT_PROPAGATION: {
                    parseDefaultContextPropagationElement(reader, subsystem);
                    break;
                }
                case ASYNC_REGISTRATION: {
                    parseAsyncRegistrationElement(reader, subsystem);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    /**
     * {@inheritDoc}          XMLExtendedStreamReader reader
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        if (node.hasDefined(HOST_NAME.getName())) {
            writer.writeStartElement(Element.HOST.getLocalName());
            HOST_NAME.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        if (node.hasDefined(ENVIRONMENT_URL.getName())) {
            writer.writeStartElement(Element.XTS_ENVIRONMENT.getLocalName());
            ENVIRONMENT_URL.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        if (node.hasDefined(DEFAULT_CONTEXT_PROPAGATION.getName())) {
            writer.writeStartElement(Element.DEFAULT_CONTEXT_PROPAGATION.getLocalName());
            DEFAULT_CONTEXT_PROPAGATION.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        if (node.hasDefined(ASYNC_REGISTRATION.getName())) {
            writer.writeStartElement(Element.ASYNC_REGISTRATION.getLocalName());
            ASYNC_REGISTRATION.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void parseHostElement(XMLExtendedStreamReader reader, ModelNode subsystem) throws XMLStreamException {
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        processAttributes(reader, (index, attribute) -> {
            required.remove(attribute);
            final String value = reader.getAttributeValue(index);
            switch (attribute) {
                case NAME:
                    HOST_NAME.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, index);
            }
        });
        // Handle elements
        ParseUtils.requireNoContent(reader);

        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }
    }

    /**
     * Handle the xts-environment element
     *
     *
     * @param reader
     * @param subsystem
     * @return ModelNode for the core-environment
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    private void parseXTSEnvironmentElement(XMLExtendedStreamReader reader, ModelNode subsystem) throws XMLStreamException {
        processAttributes(reader, (index, attribute) -> {
            final String value = reader.getAttributeValue(index);
            switch (attribute) {
                case URL:
                    ENVIRONMENT_URL.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, index);
            }
        });
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    /**
     * Handle the enable-client-handler element.
     *
     * @param reader
     * @param subsystem
     * @throws XMLStreamException
     */
    private void parseDefaultContextPropagationElement(XMLExtendedStreamReader reader, ModelNode subsystem) throws XMLStreamException {
        processAttributes(reader, (index, attribute) -> {
            final String value = reader.getAttributeValue(index);
            switch (attribute) {
                case ENABLED:
                    if (value == null || (!value.toLowerCase().equals("true") && !value.toLowerCase().equals("false"))) {
                        throw ParseUtils.invalidAttributeValue(reader, index);
                    }
                    DEFAULT_CONTEXT_PROPAGATION.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, index);
            }
        });

        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    /**
     * Handle the async-registration element.
     */
    private void parseAsyncRegistrationElement(XMLExtendedStreamReader reader, ModelNode subsystem) throws XMLStreamException {
        processAttributes(reader, (index, attribute) -> {
            final String value = reader.getAttributeValue(index);
            switch (attribute) {
                case ENABLED:
                    if (value == null || (!value.toLowerCase().equals("true") && !value.toLowerCase().equals("false"))) {
                        throw ParseUtils.invalidAttributeValue(reader, index);
                    }
                    ASYNC_REGISTRATION.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, index);
            }
        });

        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private List<Element> getExpectedElements(final XMLExtendedStreamReader reader) {
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        final List<Element> elements = new ArrayList<>();

        if (Namespace.XTS_1_0.equals(namespace)) {
            elements.add(Element.XTS_ENVIRONMENT);
        } else if (Namespace.XTS_2_0.equals(namespace)) {
            elements.add(Element.XTS_ENVIRONMENT);
            elements.add(Element.HOST);
            elements.add(Element.DEFAULT_CONTEXT_PROPAGATION);
        } else if (Namespace.XTS_3_0.equals(namespace)) {
            elements.add(Element.XTS_ENVIRONMENT);
            elements.add(Element.HOST);
            elements.add(Element.DEFAULT_CONTEXT_PROPAGATION);
            elements.add(Element.ASYNC_REGISTRATION);
        }

        return elements;
    }

    /**
     * Functional interface to provide similar functionality as {@link BiConsumer}
     * but with cought exception {@link XMLStreamException} declared.
     */
    @FunctionalInterface
    private interface AttributeProcessor<T, R> {
        void process(T t, R r) throws XMLStreamException;
    }

    /**
     * Iterating over all attributes got from the reader parameter.
     *
     * @param reader  reading the parameters from
     * @param attributeProcessorCallback  callback being processed for each attribute
     * @throws XMLStreamException troubles parsing xml
     */
    private void processAttributes(final XMLExtendedStreamReader reader, AttributeProcessor<Integer, Attribute> attributeProcessorCallback) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            // final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            attributeProcessorCallback.process(i, attribute);
        }
    }
}

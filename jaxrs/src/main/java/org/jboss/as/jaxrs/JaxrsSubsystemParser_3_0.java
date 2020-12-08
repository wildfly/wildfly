/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs;

import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class JaxrsSubsystemParser_3_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    static final PathAddress PATH_ADDRESS = PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH);

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH);
        final ModelNode subsystem = Util.createAddOperation(address);
        list.add(subsystem);
        requireNoAttributes(reader);

        final EnumSet<JaxrsElement> encountered = EnumSet.noneOf(JaxrsElement.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final JaxrsElement element = JaxrsElement.forName(reader.getLocalName());
            switch (element) {

                case JAXRS_2_0_REQUEST_MATCHING:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.JAXRS_2_0_REQUEST_MATCHING);
                    break;

                case RESTEASY_ADD_CHARSET:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_ADD_CHARSET);
                    break;

                case RESTEASY_BUFFER_EXCEPTION_ENTITY:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_BUFFER_EXCEPTION_ENTITY);
                    break;

                case RESTEASY_DISABLE_HTML_SANITIZER:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DISABLE_HTML_SANITIZER);
                    break;

                case RESTEASY_DISABLE_PROVIDERS:
                    handleList("class", reader, encountered, subsystem, JaxrsElement.RESTEASY_DISABLE_PROVIDERS);
                    break;

                case RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES);
                    break;

                case RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS);
                    break;

                case RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE);
                    break;

                case RESTEASY_GZIP_MAX_INPUT:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_GZIP_MAX_INPUT);
                    break;

                case RESTEASY_JNDI_RESOURCES:
                    handleList("jndi", reader, encountered, subsystem, JaxrsElement.RESTEASY_JNDI_RESOURCES);
                    break;

                case RESTEASY_LANGUAGE_MAPPINGS:
                    handleMap(reader, encountered, subsystem, JaxrsElement.RESTEASY_LANGUAGE_MAPPINGS);
                    break;

                case RESTEASY_MEDIA_TYPE_MAPPINGS:
                    handleMap(reader, encountered, subsystem, JaxrsElement.RESTEASY_MEDIA_TYPE_MAPPINGS);
                    break;

                case RESTEASY_MEDIA_TYPE_PARAM_MAPPING:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_MEDIA_TYPE_PARAM_MAPPING);
                    break;

                case RESTEASY_PREFER_JACKSON_OVER_JSONB:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_PREFER_JACKSON_OVER_JSONB);
                    break;

                case RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR:
                   handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR);
                   break;

                case RESTEASY_PROVIDERS:
                    handleList("class", reader, encountered, subsystem, JaxrsElement.RESTEASY_PROVIDERS);
                    break;

                case RESTEASY_RFC7232_PRECONDITIONS:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_RFC7232_PRECONDITIONS);
                    break;

                case RESTEASY_ROLE_BASED_SECURITY:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_ROLE_BASED_SECURITY);
                    break;

                case RESTEASY_SECURE_RANDOM_MAX_USE:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_SECURE_RANDOM_MAX_USE);
                    break;

                case RESTEASY_USE_BUILTIN_PROVIDERS:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_USE_BUILTIN_PROVIDERS);
                    break;

                case RESTEASY_USE_CONTAINER_FORM_PARAMS:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_USE_CONTAINER_FORM_PARAMS);
                    break;

                case RESTEASY_WIDER_REQUEST_MATCHING:
                    handleSimpleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_WIDER_REQUEST_MATCHING);
                    break;

                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(JaxrsExtension.NAMESPACE_3_0, false);
        ModelNode subsystem = context.getModelNode();
        for (AttributeDefinition attr : JaxrsAttribute.ATTRIBUTES) {
            attr.marshallAsElement(subsystem, true, streamWriter);
        }
        streamWriter.writeEndElement();
    }

    protected void handleSimpleElement(final XMLExtendedStreamReader reader,
            final EnumSet<JaxrsElement> encountered,
            final ModelNode subsystem,
            final JaxrsElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        final String value = parseElementNoAttributes(reader);
        final SimpleAttributeDefinition attribute = (SimpleAttributeDefinition) JaxrsConstants.nameToAttributeMap.get(name);
        attribute.parseAndSetParameter(value, subsystem, reader);
    }

    protected void handleList(final String tag,
            final XMLExtendedStreamReader reader,
            final EnumSet<JaxrsElement> encountered,
            final ModelNode subsystem,
            final JaxrsElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (!tag.equals(reader.getLocalName())) {
                throw unexpectedElement(reader);
            }
            String value = parseElementNoAttributes(reader);
            subsystem.get(name).add(value);
        }
    }

    protected void handleMap(final XMLExtendedStreamReader reader,
            final EnumSet<JaxrsElement> encountered,
            final ModelNode subsystem,
            final JaxrsElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        final PropertiesAttributeDefinition attribute = (PropertiesAttributeDefinition) JaxrsConstants.nameToAttributeMap.get(name);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (!"entry".equals(reader.getLocalName())) {
                throw unexpectedElement(reader);
            }
            final String[] array = requireAttributes(reader, "key");
            if (array.length != 1) {
                throw unexpectedElement(reader);
            }
            String value = reader.getElementText().trim();
            attribute.parseAndAddParameterElement(array[0], value, subsystem, reader);
        }
    }

    protected String parseElementNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        return reader.getElementText().trim();
    }
}

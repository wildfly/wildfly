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

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
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
public class JaxrsSubsystemParser_2_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

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
                    handleElement(reader, encountered, subsystem, JaxrsElement.JAXRS_2_0_REQUEST_MATCHING);
                    break;

                case RESTEASY_ADD_CHARSET:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_ADD_CHARSET);
                    break;

                case RESTEASY_BUFFER_EXCEPTION_ENTITY:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_BUFFER_EXCEPTION_ENTITY);
                    break;

                case RESTEASY_DISABLE_HTML_SANITIZER:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DISABLE_HTML_SANITIZER);
                    break;

                case RESTEASY_DISABLE_PROVIDERS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DISABLE_PROVIDERS);
                    break;

                case RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES);
                    break;

                case RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS);
                    break;

                case RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE);
                    break;

                case RESTEASY_GZIP_MAX_INPUT:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_GZIP_MAX_INPUT);
                    break;

                case RESTEASY_JNDI_RESOURCES:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_JNDI_RESOURCES);
                    break;

                case RESTEASY_LANGUAGE_MAPPINGS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_LANGUAGE_MAPPINGS);
                    break;

                case RESTEASY_MEDIA_TYPE_MAPPINGS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_MEDIA_TYPE_MAPPINGS);
                    break;

                case RESTEASY_MEDIA_TYPE_PARAM_MAPPING:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_MEDIA_TYPE_PARAM_MAPPING);
                    break;

                case RESTEASY_PROVIDERS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_PROVIDERS);
                    break;

                case RESTEASY_RESOURCES:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_RESOURCES);
                    break;

                case RESTEASY_RFC7232_PRECONDITIONS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_RFC7232_PRECONDITIONS);
                    break;

                case RESTEASY_ROLE_BASED_SECURITY:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_ROLE_BASED_SECURITY);
                    break;

                case RESTEASY_SECURE_RANDOM_MAX_USE:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_SECURE_RANDOM_MAX_USE);
                    break;

                case RESTEASY_USE_BUILTIN_PROVIDERS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_USE_BUILTIN_PROVIDERS);
                    break;

                case RESTEASY_USE_CONTAINER_FORM_PARAMS:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_USE_CONTAINER_FORM_PARAMS);
                    break;

                case RESTEASY_WIDER_REQUEST_MATCHING:
                    handleElement(reader, encountered, subsystem, JaxrsElement.RESTEASY_WIDER_REQUEST_MATCHING);
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
        context.startSubsystemElement(JaxrsExtension.NAMESPACE_2_0, false);
        ModelNode subsystem = context.getModelNode();
        for (SimpleAttributeDefinition attr : JaxrsAttribute.attributes) {
            attr.marshallAsElement(subsystem, true, streamWriter);
        }
        streamWriter.writeEndElement();
    }

    protected void handleElement(final XMLExtendedStreamReader reader,
            final EnumSet<JaxrsElement> encountered,
            final ModelNode subsystem,
            final JaxrsElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        final String value = parseElementNoAttributes(reader);
        final SimpleAttributeDefinition attribute = JaxrsConstants.nameToAttributeMap.get(name);
        attribute.parseAndSetParameter(value, subsystem, reader);
    }

    protected String parseElementNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        return reader.getElementText().trim();
    }
}

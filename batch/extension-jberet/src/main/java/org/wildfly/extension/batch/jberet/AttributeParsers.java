/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch.jberet;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Attribute parsing utilities.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class AttributeParsers {

    /**
     * An attribute parser for elements with a single {@code value} that don't allow any content within the element.
     */
    static final AttributeParser VALUE = new AttributeParser() {
        @Override
        public void parseElement(final AttributeDefinition attribute, final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
            operation.get(attribute.getName()).set(readValueAttribute(reader));
            ParseUtils.requireNoContent(reader);
        }

        @Override
        public boolean isParseAsElement() {
            return true;
        }
    };

    /**
     * Reads a {@code name} attribute on an element.
     *
     * @param reader the reader used to read the attribute with
     *
     * @return the name attribute or {@code null} if the name attribute was not defined
     *
     * @throws XMLStreamException if an XML processing error occurs
     */
    static String readNameAttribute(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return readRequiredAttributes(reader, EnumSet.of(Attribute.NAME)).get(Attribute.NAME);
    }

    /**
     * Reads a {@code value} attribute on an element.
     *
     * @param reader the reader used to read the attribute with
     *
     * @return the value attribute or {@code null} if the value attribute was not defined
     *
     * @throws XMLStreamException if an XML processing error occurs
     */
    static String readValueAttribute(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return readRequiredAttributes(reader, EnumSet.of(Attribute.VALUE)).get(Attribute.VALUE);
    }

    /**
     * Reads the required attributes from an XML configuration.
     * <p>
     * The reader must be on an element with attributes.
     * </p>
     *
     * @param reader     the reader for the attributes
     * @param attributes the required attributes
     *
     * @return a map of the required attributes with the key being the attribute and the value being the value of the
     * attribute
     *
     * @throws XMLStreamException if an XML processing error occurs
     */
    static Map<Attribute, String> readRequiredAttributes(final XMLExtendedStreamReader reader, final Set<Attribute> attributes) throws XMLStreamException {
        final int attributeCount = reader.getAttributeCount();
        final Map<Attribute, String> result = new EnumMap<>(Attribute.class);
        for (int i = 0; i < attributeCount; i++) {
            final Attribute current = Attribute.forName(reader.getAttributeLocalName(i));
            if (attributes.contains(current)) {
                if (result.put(current, reader.getAttributeValue(i)) != null) {
                    throw ParseUtils.duplicateAttribute(reader, current.getLocalName());
                }
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i, attributes.stream().map(Attribute::getLocalName).collect(Collectors.toSet()));
            }
        }
        if (result.isEmpty()) {
            throw ParseUtils.missingRequired(reader, attributes.stream().map(Attribute::getLocalName).collect(Collectors.toSet()));
        }
        return result;
    }
}
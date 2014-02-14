/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.controller;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public abstract class AttributeParser {

    /**
     * Creates a {@link org.jboss.dmr.ModelNode} using the given {@code value} after first validating the node
     * against {@link org.jboss.as.controller.AttributeDefinition#getValidator() this object's validator}., and then stores it in the given {@code operation}
     * model node as a key/value pair whose key is this attribute's getName() name}.
     * <p>
     * If {@code value} is {@code null} an {@link org.jboss.dmr.ModelType#UNDEFINED undefined} node will be stored if such a value
     * is acceptable to the validator.
     * </p>
     * <p>
     * The expected usage of this method is in parsers seeking to build up an operation to store their parsed data
     * into the configuration.
     * </p>
     *
     * @param value     the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param operation model node of type {@link org.jboss.dmr.ModelType#OBJECT} into which the parsed value should be stored
     * @param reader    {@link javax.xml.stream.XMLStreamReader} from which the {@link javax.xml.stream.XMLStreamReader#getLocation() location} from which
     *                  the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *                  the given value is invalid.
     * @throws javax.xml.stream.XMLStreamException if {@code value} is not valid
     */
    public void parseAndSetParameter(final AttributeDefinition attribute, final String value, final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException {
        ModelNode paramVal = parse(attribute, value, reader);
        operation.get(attribute.getName()).set(paramVal);
    }

    /**
     * Creates and returns a {@link org.jboss.dmr.ModelNode} using the given {@code value} after first validating the node
     * against {@link org.jboss.as.controller.AttributeDefinition#getValidator() this object's validator}.
     * <p>
     * If {@code value} is {@code null} an {@link org.jboss.dmr.ModelType#UNDEFINED undefined} node will be returned.
     * </p>
     *
     * @param value  the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param reader {@link XMLStreamReader} from which the {@link XMLStreamReader#getLocation() location} from which
     *               the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *               the given value is invalid.
     * @return {@code ModelNode} representing the parsed value
     * @throws javax.xml.stream.XMLStreamException if {@code value} is not valid
     * @see #parseAndSetParameter(org.jboss.as.controller.AttributeDefinition, String, ModelNode, XMLStreamReader)
     */
    public ModelNode parse(final AttributeDefinition attribute, final String value, final XMLStreamReader reader) throws XMLStreamException {
        try {
            return parse(attribute, value);
        } catch (OperationFailedException e) {
            throw new XMLStreamException(e.getFailureDescription().toString(), reader.getLocation());
        }
    }

    private ModelNode parse(final AttributeDefinition attribute, final String value) throws OperationFailedException {
        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null) {
            if (attribute.isAllowExpression()) {
                node = ParseUtils.parsePossibleExpression(trimmed);
            } else {
                node = new ModelNode().set(trimmed);
            }
            if (node.getType() != ModelType.EXPRESSION) {
                // Convert the string to the expected type
                switch (attribute.getType()) {
                    case BIG_DECIMAL:
                        node.set(node.asBigDecimal());
                        break;
                    case BIG_INTEGER:
                        node.set(node.asBigInteger());
                        break;
                    case BOOLEAN:
                        node.set(node.asBoolean());
                        break;
                    case BYTES:
                        node.set(node.asBytes());
                        break;
                    case DOUBLE:
                        node.set(node.asDouble());
                        break;
                    case INT:
                        node.set(node.asInt());
                        break;
                    case LONG:
                        node.set(node.asLong());
                        break;
                }
            }
        } else {
            node = new ModelNode();
        }

        attribute.getValidator().validateParameter(attribute.getXmlName(), node);

        return node;
    }

    public static final AttributeParser SIMPLE = new AttributeParser() {
    };

    public static final AttributeParser LIST = new AttributeParser() {
        @Override
        public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
            ModelNode paramVal = parse(attribute, value, reader);
            operation.get(attribute.getName()).add(paramVal);
            super.parseAndSetParameter(attribute, value, operation, reader);
        }
    };

    public static final AttributeParser STRING_LIST = new AttributeParser() {
        @Override
        public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
            if (value == null) { return; }
            for (String element : value.split(",")) {
                parseAndAddParameterElement(attribute, element, operation, reader);
            }
        }

        private void parseAndAddParameterElement(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
            ModelNode paramVal = parse(attribute, value, reader);
            operation.get(attribute.getName()).add(paramVal);
        }
    };

    public static final class DiscardOldDefaultValueParser extends AttributeParser{
        private final String value;

        public DiscardOldDefaultValueParser(String value) {
            this.value = value;
        }

        @Override
        public ModelNode parse(AttributeDefinition attribute, String value, XMLStreamReader reader) throws XMLStreamException {
            if (!this.value.equals(value)) { //if default value set, ignore it!
                return super.parse(attribute, value, reader);
            }
            return new ModelNode();
        }
    }
}

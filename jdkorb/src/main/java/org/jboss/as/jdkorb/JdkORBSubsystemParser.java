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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jdkorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.jdkorb.JdkORBSubsystemConstants.SECURITY;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * <p>
 * This class implements a parser for the JdkORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class JdkORBSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    static final JdkORBSubsystemParser INSTANCE = new JdkORBSubsystemParser();

    /**
     * <p>
     * Private constructor required by the {@code Singleton} pattern.
     * </p>
     */
    private JdkORBSubsystemParser() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> nodes) throws XMLStreamException {
        // the subsystem element has no attributes.
        requireNoAttributes(reader);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, JdkORBExtension.SUBSYSTEM_NAME);

        Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
        switch (readerNS) {
            case JdkORB_1_0: {
                this.readElement(readerNS, reader, subsystem);
                break;
            }
            default: {
                throw unexpectedElement(reader);
            }
        }

        nodes.add(subsystem);
    }

    /**
     * <p>
     * Parses the JdkORB subsystem configuration according to the XSD version 1.1 or higher.
     * </p>
     *
     * @param namespace the expected {@code Namespace} of the parsed elements.
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed subsystem configuration.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void readElement(Namespace namespace, XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (namespace != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case ORB: {
                    this.parseORBConfig(namespace, reader, node);
                    break;
                }
                case NAMING: {
                    this.parseNamingConfig(reader, node);
                    break;
                }
                case SECURITY: {
                    this.parseSecurityConfig(reader, node);
                    break;
                }
                case PROPERTIES: {
                    this.parsePropertiesConfig(namespace, reader, node);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    /**
     * <p>
     * Parses the {@code orb} section of the JdkORB subsystem configuration according to the XSD version 1.1 or higher.
     * </p>
     *
     * @param namespace the expected {@code Namespace} of the parsed elements.
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed ORB configuration.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void parseORBConfig(Namespace namespace, XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {

        // parse the orb config attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.ORB_GIOP_VERSION, Attribute.ORB_SOCKET_BINDING,
                Attribute.ORB_SSL_SOCKET_BINDING, Attribute.PERSISTENT_SERVER_ID);

        this.parseAttributes(reader, node, expectedAttributes, null);

        // parse the orb config elements.
        EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (namespace != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            // check for duplicate elements.
            if (!encountered.add(element)) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case ORB_TCP: {
                    this.parseORBTCPConfig(reader, node);
                    break;
                }
                case ORB_INITIALIZERS: {
                    this.parseORBInitializersConfig(reader, node);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    /**
     * <p>
     * Parses the ORB {@code connection} section of the JdkORB subsystem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed ORB connection configuration.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void parseORBTCPConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse the orb connection config attributes.
        EnumSet<Attribute> attributes = EnumSet.of(Attribute.ORB_TCP_HIGH_WATER_MARK,
                Attribute.ORB_TCP_NUMBER_TO_RECLAIM_PROPERTY);
        this.parseAttributes(reader, node, attributes, null);
        // the connection sub-element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the ORB {@code initializers} section of the JdkORB subsystem configuration according to the XSD version 1.1 or
     * higher.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed ORB initializers configuration.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void parseORBInitializersConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse the initializers config attributes.
        EnumSet<Attribute> attributes = EnumSet.of(Attribute.ORB_INIT_SECURITY, Attribute.ORB_INIT_TRANSACTIONS);
        this.parseAttributes(reader, node, attributes, null);
        // the initializers element doesn't have child elements.
        requireNoContent(reader);

        // if security="on" change it to security="identity"
        if (node.has(SECURITY) && node.get(SECURITY).asString().equals(SecurityAllowedValues.ON.toString())) {
            node.get(SECURITY).set(SecurityAllowedValues.IDENTITY.toString());
        }
    }

    /**
     * <p>
     * Parses the {@code naming} section of the JdkORB subsystem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed interoperability configuration.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void parseNamingConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse all naming attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.NAMING_ROOT_CONTEXT, Attribute.NAMING_EXPORT_CORBALOC);
        this.parseAttributes(reader, node, expectedAttributes, null);
        // the naming element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code security} section of the JdkORB subsystem configuration according to the XSD version 1.1 or higher.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed security configuration.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void parseSecurityConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse all security attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.SECURITY_SUPPORT_SSL, Attribute.SECURITY_SECURITY_DOMAIN,
                Attribute.SECURITY_ADD_COMPONENT_INTERCEPTOR, Attribute.SECURITY_CLIENT_SUPPORTS,
                Attribute.SECURITY_CLIENT_REQUIRES, Attribute.SECURITY_SERVER_SUPPORTS, Attribute.SECURITY_SERVER_REQUIRES);
        this.parseAttributes(reader, node, expectedAttributes, null);
        // the security element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code properties} section of the JdkORB subsystem configuration.
     * </p>
     *
     * @param namespace the expected {@code Namespace} of the parsed elements.
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed properties.
     * @throws XMLStreamException if an error occurs while parsing the XML.
     */
    private void parsePropertiesConfig(Namespace namespace, XMLExtendedStreamReader reader, ModelNode node)
            throws XMLStreamException {
        // the properties element doesn't define any attributes, just sub-elements.
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (namespace != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    // parse the property element.
                    this.parseGenericProperty(reader, node.get(JdkORBSubsystemConstants.PROPERTIES));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    /**
     * <p>
     * Parses a {@code property} element according to the XSD version 1.0 or higher and adds the name/value pair to the
     * specified {@code ModelNode}.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that contains all parsed ORB properties.
     * @throws javax.xml.stream.XMLStreamException if an error occurs while parsing the XML.
     */
    private void parseGenericProperty(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        String name = null;
        String val = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.PROP_NAME, Attribute.PROP_VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case PROP_NAME: {
                    name = value;
                    break;
                }
                case PROP_VALUE: {
                    val = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        node.add(name, val);
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses all attributes from the current element and sets them in the specified {@code ModelNode}.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node the {@code ModelNode} that will hold the parsed attributes.
     * @param expectedAttributes an {@code EnumSet} containing all expected attributes. If the parsed attribute is not one of
     *        the expected attributes, an exception is thrown.
     * @param requiredAttributes an {@code EnumSet} containing all required attributes. If a required attribute is not found, an
     *        exception is thrown.
     * @throws XMLStreamException if an error occurs while parsing the XML, if an attribute is not one of the expected
     *         attributes or if one of the required attributes is not parsed.
     */
    private void parseAttributes(XMLExtendedStreamReader reader, ModelNode node, EnumSet<Attribute> expectedAttributes,
            EnumSet<Attribute> requiredAttributes) throws XMLStreamException {

        EnumSet<Attribute> parsedAttributes = EnumSet.noneOf(Attribute.class);
        if (requiredAttributes == null) {
            requiredAttributes = EnumSet.noneOf(Attribute.class);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            // check for unexpected attributes.
            if (!expectedAttributes.contains(attribute))
                throw unexpectedAttribute(reader, i);
            // check for duplicate attributes.
            if (!parsedAttributes.add(attribute)) {
                throw duplicateAttribute(reader, attribute.getLocalName());
            }
            requiredAttributes.remove(attribute);
            ((SimpleAttributeDefinition) JdkORBSubsystemDefinitions.valueOf(attribute.getLocalName())).parseAndSetParameter(
                    attrValue, node, reader);
        }

        // throw an exception if a required attribute wasn't found.
        if (!requiredAttributes.isEmpty()) {
            throw missingRequired(reader, requiredAttributes);
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        // write the orb configuration section if there are any orb properties to be written.
        this.writeORBConfig(writer, node);

        // write the naming configuration section if there are any naming properties to be written.
        this.writeNamingConfig(writer, node);

        // write the security configuration section if there are any security properties to be written.
        this.writeSecurityConfig(writer, node);

        // write all defined generic properties.
        String properties = JdkORBSubsystemConstants.PROPERTIES;
        if (node.hasDefined(properties)) {
            this.writeGenericProperties(writer, node.get(properties));
        }
        writer.writeEndElement(); // End of subsystem element
    }

    /**
     * <p>
     * Writes the {@code orb} section of the JdkORB subsystem configuration using the contents of the provided {@code ModelNode}
     * .
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node the {@code ModelNode} that might contain ORB configuration properties.
     * @throws XMLStreamException if an error occurs while writing the ORB configuration.
     */
    private void writeORBConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {

        boolean writeORB = this.isWritable(node, JdkORBSubsystemDefinitions.ORB_ATTRIBUTES);
        boolean writeORBConnection = this.isWritable(node, JdkORBSubsystemDefinitions.ORB_TCP_ATTRIBUTES);
        boolean writeORBInitializer = this.isWritable(node, JdkORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES);

        // if no connection or initializers properties are available, just write the orb properties (if any) in an empty
        // element.
        if (!writeORBConnection && !writeORBInitializer) {
            if (writeORB) {
                writer.writeEmptyElement(JdkORBSubsystemConstants.ORB);
                this.writeAttributes(writer, node, JdkORBSubsystemDefinitions.ORB_ATTRIBUTES);
            }
        }
        // otherwise write the orb element with the appropriate sub-elements.
        else {
            writer.writeStartElement(JdkORBSubsystemConstants.ORB);
            this.writeAttributes(writer, node, JdkORBSubsystemDefinitions.ORB_ATTRIBUTES);
            if (writeORBConnection) {
                writer.writeEmptyElement(JdkORBSubsystemConstants.ORB_TCP);
                this.writeAttributes(writer, node, JdkORBSubsystemDefinitions.ORB_TCP_ATTRIBUTES);
            }
            if (writeORBInitializer) {
                writer.writeEmptyElement(JdkORBSubsystemConstants.ORB_INIT);
                this.writeAttributes(writer, node, JdkORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES);
            }
            writer.writeEndElement();
        }
    }

    /**
     * <p>
     * Writes the {@code naming} section of the JdkORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node the {@code ModelNode} that contains the naming configuration properties.
     * @throws XMLStreamException if an error occurs while writing the interop configuration.
     */
    private void writeNamingConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        boolean writeNaming = this.isWritable(node, JdkORBSubsystemDefinitions.NAMING_ATTRIBUTES);
        if (writeNaming) {
            writer.writeEmptyElement(JdkORBSubsystemConstants.NAMING);
            this.writeAttributes(writer, node, JdkORBSubsystemDefinitions.NAMING_ATTRIBUTES);
        }
    }

    /**
     * <p>
     * Writes the {@code security} section of the JdkORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node the {@code ModelNode} that contains the security configuration properties.
     * @throws XMLStreamException if an error occurs while writing the security configuration.
     */
    private void writeSecurityConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        boolean writeSecurity = this.isWritable(node, JdkORBSubsystemDefinitions.SECURITY_ATTRIBUTES);
        if (writeSecurity) {
            writer.writeEmptyElement(SECURITY);
            this.writeAttributes(writer, node, JdkORBSubsystemDefinitions.SECURITY_ATTRIBUTES);
        }
    }

    /**
     * <p>
     * Writes a {@code property} element for each generic property contained in the specified {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node   the {@code ModelNode} that contains all properties to be written.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while writing the property elements.
     */
    private void writeGenericProperties(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        writer.writeStartElement(JdkORBSubsystemConstants.PROPERTIES);
        for (Property prop : node.asPropertyList()) {
            writer.writeEmptyElement(JdkORBSubsystemConstants.PROPERTY);
            writer.writeAttribute(JdkORBSubsystemConstants.PROPERTY_NAME, prop.getName());
            writer.writeAttribute(JdkORBSubsystemConstants.PROPERTY_VALUE, prop.getValue().asString());
        }
        writer.writeEndElement();
    }

    /**
     * <p>
     * Writes the attributes contained in the specified {@code ModelNode} to the current element.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node the {@code ModelNode} that contains the attributes to be written.
     * @param attributes the list of attributes to be written if they have been defined.
     * @throws XMLStreamException if an error occurs while writing the attributes to the current element.
     */
    private void writeAttributes(XMLExtendedStreamWriter writer, ModelNode node, List<SimpleAttributeDefinition> attributes)
            throws XMLStreamException {
        for (SimpleAttributeDefinition definition : attributes)
            definition.marshallAsAttribute(node, writer);
    }

    /**
     * <p>
     * Iterates through the specified attribute definitions and checks if any of the attributes can be written to XML by
     * verifying if the attribute has been defined in the supplied node.
     * </p>
     *
     * @param node the {@code ModelNode} that contains the configuration attributes.
     * @param attributeDefinitions the {@code AttributeDefinition}s of the attributes that might be writable.
     * @return {@code true} if the node has defined any of the attributes; {@code false} otherwise.
     */
    private boolean isWritable(ModelNode node, List<SimpleAttributeDefinition> attributeDefinitions) {
        boolean isWritable = false;
        for (SimpleAttributeDefinition attributeDefinition : attributeDefinitions) {
            if (attributeDefinition.isMarshallable(node)) {
                isWritable = true;
                break;
            }
        }
        return isWritable;
    }

    // helper enum types that encapsulate the subsystem namespace, elements, and attributes.

    /**
     * <p>
     * Enumeration of available JdkORB subsystem namespaces.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     */
    enum Namespace {

        UNKNOWN(null), JdkORB_1_0("urn:jboss:domain:jdkorb:1.0");

        static final Namespace CURRENT = JdkORB_1_0;

        private final String namespaceURI;

        /**
         * <p>
         * {@code Namespace} constructor. Sets the namespace {@code URI}.
         * </p>
         *
         * @param namespaceURI a {@code String} representing the namespace {@code URI}.
         */
        private Namespace(final String namespaceURI) {
            this.namespaceURI = namespaceURI;
        }

        /**
         * <p>
         * Obtains the {@code URI} of this namespace.
         * </p>
         *
         * @return a {@code String} representing the namespace {@code URI}.
         */
        String getUriString() {
            return namespaceURI;
        }

        // a map that caches all available namespaces by URI.
        private static final Map<String, Namespace> MAP;

        static {
            final Map<String, Namespace> map = new HashMap<String, Namespace>();
            for (final Namespace namespace : values()) {
                final String name = namespace.getUriString();
                if (name != null)
                    map.put(name, namespace);
            }
            MAP = map;
        }

        /**
         * <p>
         * Gets the {@code Namespace} identified by the specified {@code URI}.
         * </p>
         *
         * @param uri a {@code String} representing the namespace {@code URI}.
         * @return the {@code Namespace} identified by the {@code URI}. If no namespace can be found, the
         *         {@code Namespace.UNKNOWN} type is returned.
         */
        static Namespace forUri(final String uri) {
            final Namespace element = MAP.get(uri);
            return element == null ? UNKNOWN : element;
        }
    }

    /**
     * <p>
     * Enumeration of the JdkORB subsystem XML configuration elements.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
     */
    enum Element {

        UNKNOWN(null),

        // elements used to configure the ORB.
        ORB(JdkORBSubsystemConstants.ORB), ORB_TCP(JdkORBSubsystemConstants.ORB_TCP), ORB_INITIALIZERS(
                JdkORBSubsystemConstants.ORB_INIT),

        // elements used to configure the naming service, ORB interoperability and ORB security.
        NAMING(JdkORBSubsystemConstants.NAMING), SECURITY(JdkORBSubsystemConstants.SECURITY),

        // elements used to configure generic properties.
        PROPERTIES(JdkORBSubsystemConstants.PROPERTIES), PROPERTY(JdkORBSubsystemConstants.PROPERTY);

        private final String name;

        /**
         * <p>
         * {@code Element} constructor. Sets the element name.
         * </p>
         *
         * @param name a {@code String} representing the local name of the element.
         */
        Element(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Obtains the local name of this element.
         * </p>
         *
         * @return a {@code String} representing the element's local name.
         */
        public String getLocalName() {
            return name;
        }

        // a map that caches all available elements by name.
        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null)
                    map.put(name, element);
            }
            MAP = map;
        }

        /**
         * <p>
         * Gets the {@code Element} identified by the specified name.
         * </p>
         *
         * @param localName a {@code String} representing the local name of the element.
         * @return the {@code Element} identified by the name. If no attribute can be found, the {@code Element.UNKNOWN} type is
         *         returned.
         */
        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    /**
     * <p>
     * Enumeration of the JdkORB subsystem XML configuration attributes.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     */
    enum Attribute {

        UNKNOWN(null),

        // attributes of the orb element.
        ORB_GIOP_VERSION(
                JdkORBSubsystemConstants.ORB_GIOP_VERSION), ORB_SOCKET_BINDING(JdkORBSubsystemConstants.ORB_SOCKET_BINDING), ORB_SSL_SOCKET_BINDING(
                JdkORBSubsystemConstants.ORB_SSL_SOCKET_BINDING), PERSISTENT_SERVER_ID(
                JdkORBSubsystemConstants.ORB_PERSISTENT_SERVER_ID),

        // attributes of the tcp element.
        ORB_TCP_HIGH_WATER_MARK(JdkORBSubsystemConstants.TCP_HIGH_WATER_MARK), ORB_TCP_NUMBER_TO_RECLAIM_PROPERTY(
                JdkORBSubsystemConstants.TCP_NUMBER_TO_RECLAIM),

        // attributes of the initializers element.
        ORB_INIT_SECURITY(JdkORBSubsystemConstants.ORB_INIT_SECURITY), ORB_INIT_TRANSACTIONS(
                JdkORBSubsystemConstants.ORB_INIT_TRANSACTIONS),

        // attributes of the naming element - the ORB service will build the relevant JdkORB properties from these values.
        NAMING_EXPORT_CORBALOC(JdkORBSubsystemConstants.NAMING_EXPORT_CORBALOC), NAMING_ROOT_CONTEXT(
                JdkORBSubsystemConstants.NAMING_ROOT_CONTEXT),

        // attributes of the security element.
        SECURITY_SUPPORT_SSL(JdkORBSubsystemConstants.SECURITY_SUPPORT_SSL), SECURITY_SECURITY_DOMAIN(
                JdkORBSubsystemConstants.SECURITY_SECURITY_DOMAIN), SECURITY_ADD_COMPONENT_INTERCEPTOR(
                JdkORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR), SECURITY_CLIENT_SUPPORTS(
                JdkORBSubsystemConstants.SECURITY_CLIENT_SUPPORTS), SECURITY_CLIENT_REQUIRES(
                JdkORBSubsystemConstants.SECURITY_CLIENT_REQUIRES), SECURITY_SERVER_SUPPORTS(
                JdkORBSubsystemConstants.SECURITY_SERVER_SUPPORTS), SECURITY_SERVER_REQUIRES(
                JdkORBSubsystemConstants.SECURITY_SERVER_REQUIRES),
        // if enabled the ORB service will configure JdkORB to use the JBoss SSL socket factory classes by building the
        // appropriate properties.
        SECURITY_USE_DOMAIN_SF(JdkORBSubsystemConstants.SECURITY_USE_DOMAIN_SF), SECURITY_USE_DOMAIN_SSF(
                JdkORBSubsystemConstants.SECURITY_USE_DOMAIN_SSF),

        // attributes of the generic property element.
        PROP_NAME(JdkORBSubsystemConstants.PROPERTY_NAME), PROP_VALUE(JdkORBSubsystemConstants.PROPERTY_VALUE);

        private final String name;

        /**
         * <p>
         * {@code Attribute} constructor. Sets the attribute name.
         * </p>
         *
         * @param name a {@code String} representing the local name of the attribute.
         */
        Attribute(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Obtains the local name of this attribute.
         * </p>
         *
         * @return a {@code String} representing the attribute local name.
         */
        public String getLocalName() {
            return this.name;
        }

        // a map that caches all available attributes by name.
        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
            for (Attribute attribute : values()) {
                final String name = attribute.name;
                if (name != null)
                    map.put(name, attribute);
            }
            MAP = map;
        }

        /**
         * <p>
         * Gets the {@code Attribute} identified by the specified name.
         * </p>
         *
         * @param localName a {@code String} representing the local name of the attribute.
         * @return the {@code Attribute} identified by the name. If no attribute can be found, the {@code Attribute.UNKNOWN}
         *         type is returned.
         */
        public static Attribute forName(String localName) {
            final Attribute attribute = MAP.get(localName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }
}
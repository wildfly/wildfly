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

package org.jboss.as.jacorb;

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
 * This class implements a parser for the JacORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JacORBSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    static final JacORBSubsystemParser INSTANCE = new JacORBSubsystemParser();

    /**
     * <p>
     * Private constructor required by the {@code Singleton} pattern.
     * </p>
     */
    private JacORBSubsystemParser() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> nodes) throws XMLStreamException {
        // the subsystem element has no attributes.
        requireNoAttributes(reader);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME);

        Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
        switch (readerNS) {
            case JacORB_1_0: {
                this.readElement_1_0(reader, subsystem);
                break;
            }
            case JacORB_1_1: {
                this.readElement_1_1(reader, subsystem);
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
     * Parses the JacORB subsystem configuration according to the XSD version 1.0.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed subsystem configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void readElement_1_0(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (Namespace.JacORB_1_0 != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            // there can be multiple property elements.
            if (!encountered.add(element) && element != Element.PROPERTY) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case ORB: {
                    this.parseORBConfig_1_0(reader, node);
                    break;
                }
                case POA: {
                    this.parsePOAConfig(reader, node);
                    break;
                }
                case INTEROP: {
                    this.parseInteropConfig(reader, node);
                    break;
                }
                case SECURITY: {
                    this.parseSecurityConfig(reader, node);
                    break;
                }
                case PROPERTY: {
                    ModelNode propertiesNode = node.get(JacORBSubsystemConstants.PROPERTIES);
                    this.parseGenericProperty_1_0(reader, propertiesNode);
                    break;
                }
                case ORB_INITIALIZERS: {
                    this.parseORBInitializersConfig_1_0(reader, node);
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
     * Parses the JacORB subsystem configuration according to the XSD version 1.1.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed subsystem configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void readElement_1_1(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (Namespace.JacORB_1_1 != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case ORB: {
                    this.parseORBConfig_1_1(reader, node);
                    break;
                }
                case POA: {
                    this.parsePOAConfig(reader, node);
                    break;
                }
                case NAMING: {
                    this.parseNamingConfig(reader, node);
                    break;
                }
                case INTEROP: {
                    this.parseInteropConfig(reader, node);
                    break;
                }
                case SECURITY: {
                    this.parseSecurityConfig(reader, node);
                    break;
                }
                case PROPERTIES: {
                    this.parsePropertiesConfig(reader, node);
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
     * Parses the {@code orb} section of the JacORB subsystem configuration according to the XSD version 1.0.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed ORB configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseORBConfig_1_0(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {

        // parse the orb config attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.NAME, Attribute.ORB_PRINT_VERSION,
                Attribute.ORB_GIOP_MINOR_VERSION, Attribute.ORB_USE_BOM, Attribute.ORB_USE_IMR,
                Attribute.ORB_CACHE_POA_NAMES, Attribute.ORB_CACHE_TYPECODES);
        this.parseAttributes(reader, node, expectedAttributes, null);

        // parse the orb config elements.
        EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (Namespace.JacORB_1_0 != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            // check for duplicate elements.
            if (!encountered.add(element)) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case ORB_CONNECTION: {
                    this.parseORBConnectionConfig(reader, node);
                    break;
                }
                case NAMING: {
                    this.parseNamingConfig(reader, node);
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
     * Parses the {@code orb} section of the JacORB subsystem configuration according to the XSD version 1.1.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed ORB configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseORBConfig_1_1(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {

        // parse the orb config attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.NAME, Attribute.ORB_PRINT_VERSION,
                Attribute.ORB_GIOP_MINOR_VERSION, Attribute.ORB_USE_BOM, Attribute.ORB_USE_IMR,
                Attribute.ORB_CACHE_POA_NAMES, Attribute.ORB_CACHE_TYPECODES);
        this.parseAttributes(reader, node, expectedAttributes, null);

        // parse the orb config elements.
        EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // check the element namespace.
            if (Namespace.JacORB_1_1 != Namespace.forUri(reader.getNamespaceURI()))
                throw unexpectedElement(reader);

            final Element element = Element.forName(reader.getLocalName());
            // check for duplicate elements.
            if (!encountered.add(element)) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case ORB_CONNECTION: {
                    this.parseORBConnectionConfig(reader, node);
                    break;
                }
                case ORB_INITIALIZERS: {
                    this.parseORBInitializersConfig_1_1(reader, node);
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
     * Parses the ORB {@code connection} section of the JacORB susbsytem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed ORB connection configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseORBConnectionConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse the orb connection config attributes.
        EnumSet<Attribute> attributes = EnumSet.of(Attribute.ORB_CONN_RETRIES, Attribute.ORB_CONN_RETRY_INTERVAL,
                Attribute.ORB_CONN_CLIENT_TIMEOUT, Attribute.ORB_CONN_SERVER_TIMEOUT,
                Attribute.ORB_CONN_MAX_SERVER_CONNECTIONS, Attribute.ORB_CONN_MAX_MANAGED_BUF_SIZE,
                Attribute.ORB_CONN_OUTBUF_SIZE, Attribute.ORB_CONN_OUTBUF_CACHE_TIMEOUT);
        this.parseAttributes(reader, node, attributes, null);
        // the connection sub-element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the ORB {@code initializers} section of the JacORB subsystem configuration according to the XSD version 1.0.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed ORB initializers configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseORBInitializersConfig_1_0(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        requireNoAttributes(reader);
        // read the element text - a comma-separated list of initializers.
        String initializersList = reader.getElementText();
        if (initializersList != null) {
            String[] initializers = initializersList.split(",");
            // read each configured initializer and set the appropriate values in the model node.
            for (String initializer : initializers) {
                SimpleAttributeDefinition definition = JacORBSubsystemDefinitions.valueOf(initializer);
                if (definition != null && JacORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES.contains(definition))
                    node.get(definition.getName()).set("on");
                else
                    throw new XMLStreamException(
                            "Unexpected element value. Should be one of [security,transactions]");
            }
        }
    }

    /**
     * <p>
     * Parses the ORB {@code initializers} section of the JacORB subsystem configuration according to the XSD version 1.1.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed ORB initializers configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseORBInitializersConfig_1_1(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse the initializers config attributes.
        EnumSet<Attribute> attributes = EnumSet.of(Attribute.ORB_INIT_SECURITY, Attribute.ORB_INIT_TRANSACTIONS);
        this.parseAttributes(reader, node, attributes, null);
        // the initializers element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code poa} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed POA configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parsePOAConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {

        // parse the poa config attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.POA_MONITORING, Attribute.POA_QUEUE_WAIT,
                Attribute.POA_QUEUE_MIN, Attribute.POA_QUEUE_MAX);
        this.parseAttributes(reader, node, expectedAttributes, null);

        // parse the poa config elements.
        EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            // check for duplicate elements.
            if (!encountered.add(element)) {
                throw duplicateNamedElement(reader, element.getLocalName());
            }
            switch (element) {
                case POA_REQUEST_PROC: {
                    // parse the poa request-processors config attributes.
                    EnumSet<Attribute> attributes =
                            EnumSet.of(Attribute.POA_REQUEST_PROC_POOL_SIZE, Attribute.POA_REQUEST_PROC_MAX_THREADS);
                    this.parseAttributes(reader, node, attributes, EnumSet.copyOf(attributes));
                    // the request-processors element doesn't have child elements.
                    requireNoContent(reader);
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
     * Parses the {@code naming} section of the JacORB susbsytem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed interoperability configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
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
     * Parses the {@code interop} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed interoperability configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseInteropConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse all interop attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.INTEROP_SUN, Attribute.INTEROP_COMET,
                Attribute.INTEROP_IONA, Attribute.INTEROP_CHUNK_RMI_VALUETYPES, Attribute.INTEROP_LAX_BOOLEAN_ENCODING,
                Attribute.INTEROP_INDIRECTION_ENCODING_DISABLE, Attribute.INTEROP_STRICT_CHECK_ON_TC_CREATION);
        this.parseAttributes(reader, node, expectedAttributes, null);
        // the interop element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code security} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed security configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseSecurityConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // parse all security attributes.
        EnumSet<Attribute> expectedAttributes = EnumSet.of(Attribute.SECURITY_SUPPORT_SSL, Attribute.SECURITY_SECURITY_DOMAIN,
                Attribute.SECURITY_ADD_COMPONENT_INTERCEPTOR, Attribute.SECURITY_CLIENT_SUPPORTS, Attribute.SECURITY_CLIENT_REQUIRES,
                Attribute.SECURITY_SERVER_SUPPORTS, Attribute.SECURITY_SERVER_REQUIRES, Attribute.SECURITY_USE_DOMAIN_SF,
                Attribute.SECURITY_USE_DOMAIN_SSF);
        this.parseAttributes(reader, node, expectedAttributes, null);
        // the security element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code properties} section of the JacORB subssytem configuration.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that will hold the parsed properties.
     * @throws XMLStreamException if an error occurs while parsing the XML.
     */
    private void parsePropertiesConfig(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        // the properties element doesn't define any attributes, just sub-elements.
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JacORB_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PROPERTY: {
                            // parse the property element.
                            this.parseGenericProperty_1_1(reader, node.get(JacORBSubsystemConstants.PROPERTIES));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
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
     * Parses a {@code property} element according to the XSD version 1.1 and adds the name/value pair to the specified
     * {@code ModelNode}.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that contains all parsed ORB properties.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseGenericProperty_1_0(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        String name = null;
        String val = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.PROP_KEY, Attribute.PROP_VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case PROP_KEY: {
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
     * Parses a {@code property} element according to the XSD version 1.1 and adds the name/value pair to the specified
     * {@code ModelNode}.
     * </p>
     *
     * @param reader the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node   the {@code ModelNode} that contains all parsed ORB properties.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseGenericProperty_1_1(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        String name = null;
        String val = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.PROP_VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
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
     * @param reader             the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node               the {@code ModelNode} that will hold the parsed attributes.
     * @param expectedAttributes an {@code EnumSet} containing all expected attributes. If the parsed attribute is not
     *                           one of the expected attributes, an exception is thrown.
     * @param requiredAttributes an {@code EnumSet} containing all required attributes. If a required attribute is not
     *                           found, an exception is thrown.
     * @throws XMLStreamException if an error occurs while parsing the XML, if an attribute is not one of the expected
     *                            attributes or if one of the required attributes is not parsed.
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
            JacORBSubsystemDefinitions.valueOf(attribute.getLocalName()).parseAndSetParameter(attrValue, node, reader);
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

        // write the poa configuration section if there are any poa properties to be written.
        this.writePOAConfig(writer, node);

        // write the naming configuration section if there are any naming properties to be written.
        this.writeNamingConfig(writer, node);

        // write the interop configuration section if there are any interop properties to be written.
        this.writeInteropConfig(writer, node);

        // write the security configuration section if there are any security properties to be written.
        this.writeSecurityConfig(writer, node);

        // write all defined generic properties.
        String properties = JacORBSubsystemConstants.PROPERTIES;
        if (node.hasDefined(properties)) {
            this.writeGenericProperties(writer, node.get(properties));
        }
        writer.writeEndElement(); // End of subsystem element
    }

    /**
     * <p>
     * Writes the {@code orb} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node   the {@code ModelNode} that might contain ORB configuration properties.
     * @throws XMLStreamException if an error occurs while writing the ORB configuration.
     */
    private void writeORBConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {

        boolean writeORB = this.isWritable(node, JacORBSubsystemDefinitions.ORB_ATTRIBUTES);
        boolean writeORBConnection = this.isWritable(node, JacORBSubsystemDefinitions.ORB_CONN_ATTRIBUTES);
        boolean writeORBInitialzers = this.isWritable(node, JacORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES);

        // if no connection or initializers properties are available, just write the orb properties (if any) in an empty element.
        if (!writeORBConnection && !writeORBInitialzers) {
            if (writeORB) {
                writer.writeEmptyElement(JacORBSubsystemConstants.ORB);
                this.writeAttributes(writer, node, JacORBSubsystemDefinitions.ORB_ATTRIBUTES);
            }
        }
        // otherwise write the orb element with the appropriate sub-elements.
        else {
            writer.writeStartElement(JacORBSubsystemConstants.ORB);
            this.writeAttributes(writer, node, JacORBSubsystemDefinitions.ORB_ATTRIBUTES);
            if (writeORBConnection) {
                writer.writeEmptyElement(JacORBSubsystemConstants.ORB_CONN);
                this.writeAttributes(writer, node, JacORBSubsystemDefinitions.ORB_CONN_ATTRIBUTES);
            }
            if (writeORBInitialzers) {
                writer.writeEmptyElement(JacORBSubsystemConstants.ORB_INIT);
                this.writeAttributes(writer, node, JacORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES);
            }
            writer.writeEndElement();
        }
    }

    /**
     * <p>
     * Writes the {@code poa} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node   the {@code ModelNode} that might contain POA configuration properties.
     * @throws XMLStreamException if an error occurs while writing the POA configuration.
     */
    private void writePOAConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {

        boolean writePOA = this.isWritable(node, JacORBSubsystemDefinitions.POA_ATTRIBUTES);
        boolean writePOARP = this.isWritable(node, JacORBSubsystemDefinitions.POA_RP_ATTRIBUTES);

        // if no request processor properties are available, just write the poa properties (if any) in an empty element.
        if (!writePOARP) {
            if (writePOA) {
                writer.writeEmptyElement(JacORBSubsystemConstants.POA);
                this.writeAttributes(writer, node, JacORBSubsystemDefinitions.POA_ATTRIBUTES);
            }
        }
        // otherwise write the poa element with the appropriate sub-elements.
        else {
            writer.writeStartElement(JacORBSubsystemConstants.POA);
            this.writeAttributes(writer, node, JacORBSubsystemDefinitions.POA_ATTRIBUTES);
            writer.writeEmptyElement(JacORBSubsystemConstants.POA_RP);
            this.writeAttributes(writer, node, JacORBSubsystemDefinitions.POA_RP_ATTRIBUTES);
            writer.writeEndElement();
        }
    }

    /**
     * <p>
     * Writes the {@code naming} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node   the {@code ModelNode} that contains the naming configuration properties.
     * @throws XMLStreamException if an error occurs while writing the interop configuration.
     */
    private void writeNamingConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        boolean writeNaming = this.isWritable(node, JacORBSubsystemDefinitions.NAMING_ATTRIBUTES);
        if (writeNaming) {
            writer.writeEmptyElement(JacORBSubsystemConstants.NAMING);
            this.writeAttributes(writer, node, JacORBSubsystemDefinitions.NAMING_ATTRIBUTES);
        }
    }

    /**
     * <p>
     * Writes the {@code interop} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node   the {@code ModelNode} that contains the interoperability configuration properties.
     * @throws XMLStreamException if an error occurs while writing the interop configuration.
     */
    private void writeInteropConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        boolean writeInterop = this.isWritable(node, JacORBSubsystemDefinitions.INTEROP_ATTRIBUTES);
        if (writeInterop) {
            writer.writeEmptyElement(JacORBSubsystemConstants.INTEROP);
            this.writeAttributes(writer, node, JacORBSubsystemDefinitions.INTEROP_ATTRIBUTES);
        }
    }

    /**
     * <p>
     * Writes the {@code security} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node   the {@code ModelNode} that contains the security configuration properties.
     * @throws XMLStreamException if an error occurs while writing the security configuration.
     */
    private void writeSecurityConfig(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        boolean writeSecurity = this.isWritable(node, JacORBSubsystemDefinitions.SECURITY_ATTRIBUTES);
        if (writeSecurity) {
            writer.writeEmptyElement(JacORBSubsystemConstants.SECURITY);
            this.writeAttributes(writer, node, JacORBSubsystemDefinitions.SECURITY_ATTRIBUTES);
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
        writer.writeStartElement(JacORBSubsystemConstants.PROPERTIES);
        for (Property prop : node.asPropertyList()) {
            writer.writeEmptyElement(JacORBSubsystemConstants.PROPERTY);
            writer.writeAttribute(JacORBSubsystemConstants.NAME, prop.getName());
            writer.writeAttribute(JacORBSubsystemConstants.PROPERTY_VALUE, prop.getValue().asString());
        }
        writer.writeEndElement();
    }

    /**
     * <p>
     * Writes the attributes contained in the specified {@code ModelNode} to the current element.
     * </p>
     *
     * @param writer     the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param node       the {@code ModelNode} that contains the attributes to be written.
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
     * Iterates through the specified attribute definitions and checks if any of the attributes can be written to XML
     * by verifying if the attribute has been defined in the supplied node.
     * </p>
     *
     * @param node                 the {@code ModelNode} that contains the configuration attributes.
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
     * Enumeration of available JacORB subsystem namespaces.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     */
    enum Namespace {

        UNKNOWN(null),
        JacORB_1_0("urn:jboss:domain:jacorb:1.0"),
        JacORB_1_1("urn:jboss:domain:jacorb:1.1");

        static final Namespace CURRENT = JacORB_1_1;

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
     * Enumeration of the JacORB subsystem XML configuration elements.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     */
    enum Element {

        UNKNOWN(null),

        // elements used to configure the ORB.
        ORB(JacORBSubsystemConstants.ORB),
        ORB_CONNECTION(JacORBSubsystemConstants.ORB_CONN),
        ORB_INITIALIZERS(JacORBSubsystemConstants.ORB_INIT),

        // elements used to configure the POA.
        POA(JacORBSubsystemConstants.POA),
        POA_REQUEST_PROC(JacORBSubsystemConstants.POA_RP),

        // elements used to configure the naming service, ORB interoperability and ORB security.
        NAMING(JacORBSubsystemConstants.NAMING),
        INTEROP(JacORBSubsystemConstants.INTEROP),
        SECURITY(JacORBSubsystemConstants.SECURITY),

        // elements used to configure generic properties.
        PROPERTIES(JacORBSubsystemConstants.PROPERTIES),
        PROPERTY(JacORBSubsystemConstants.PROPERTY);

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
         * @return the {@code Element} identified by the name. If no attribute can be found, the {@code Element.UNKNOWN}
         *         type is returned.
         */
        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    /**
     * <p>
     * Enumeration of the JacORB subsystem XML configuration attributes.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     */
    enum Attribute {

        UNKNOWN(null),

        // attributes of the orb element.
        NAME(JacORBSubsystemConstants.NAME),
        ORB_PRINT_VERSION(JacORBSubsystemConstants.ORB_PRINT_VERSION),
        ORB_USE_IMR(JacORBSubsystemConstants.ORB_USE_IMR),
        ORB_USE_BOM(JacORBSubsystemConstants.ORB_USE_BOM),
        ORB_CACHE_TYPECODES(JacORBSubsystemConstants.ORB_CACHE_TYPECODES),
        ORB_CACHE_POA_NAMES(JacORBSubsystemConstants.ORB_CACHE_POA_NAMES),
        ORB_GIOP_MINOR_VERSION(JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION),

        // attributes of the connection element.
        ORB_CONN_RETRIES(JacORBSubsystemConstants.ORB_CONN_RETRIES),
        ORB_CONN_RETRY_INTERVAL(JacORBSubsystemConstants.ORB_CONN_RETRY_INTERVAL),
        ORB_CONN_CLIENT_TIMEOUT(JacORBSubsystemConstants.ORB_CONN_CLIENT_TIMEOUT),
        ORB_CONN_SERVER_TIMEOUT(JacORBSubsystemConstants.ORB_CONN_SERVER_TIMEOUT),
        ORB_CONN_MAX_SERVER_CONNECTIONS(JacORBSubsystemConstants.ORB_CONN_MAX_SERVER_CONNECTIONS),
        ORB_CONN_MAX_MANAGED_BUF_SIZE(JacORBSubsystemConstants.ORB_CONN_MAX_MANAGED_BUF_SIZE),
        ORB_CONN_OUTBUF_SIZE(JacORBSubsystemConstants.ORB_CONN_OUTBUF_SIZE),
        ORB_CONN_OUTBUF_CACHE_TIMEOUT(JacORBSubsystemConstants.ORB_CONN_OUTBUF_CACHE_TIMEOUT),

        // attributes of the initializers element.
        ORB_INIT_SECURITY(JacORBSubsystemConstants.ORB_INIT_SECURITY),
        ORB_INIT_TRANSACTIONS(JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS),

        // attributes of the poa element.
        POA_MONITORING(JacORBSubsystemConstants.POA_MONITORING),
        POA_QUEUE_WAIT(JacORBSubsystemConstants.POA_QUEUE_WAIT),
        POA_QUEUE_MIN(JacORBSubsystemConstants.POA_QUEUE_MIN),
        POA_QUEUE_MAX(JacORBSubsystemConstants.POA_QUEUE_MAX),

        // attributes of the request-processor element.
        POA_REQUEST_PROC_POOL_SIZE(JacORBSubsystemConstants.POA_RP_POOL_SIZE),
        POA_REQUEST_PROC_MAX_THREADS(JacORBSubsystemConstants.POA_RP_MAX_THREADS),

        // attributes of the naming element - the ORB service will build the relevant JacORB properties from these values.
        NAMING_EXPORT_CORBALOC(JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC),
        NAMING_ROOT_CONTEXT(JacORBSubsystemConstants.NAMING_ROOT_CONTEXT),

        // attributes of the interop element.
        INTEROP_SUN(JacORBSubsystemConstants.INTEROP_SUN),
        INTEROP_COMET(JacORBSubsystemConstants.INTEROP_COMET),
        INTEROP_IONA(JacORBSubsystemConstants.INTEROP_IONA),
        INTEROP_CHUNK_RMI_VALUETYPES(JacORBSubsystemConstants.INTEROP_CHUNK_RMI_VALUETYPES),
        INTEROP_LAX_BOOLEAN_ENCODING(JacORBSubsystemConstants.INTEROP_LAX_BOOLEAN_ENCODING),
        INTEROP_INDIRECTION_ENCODING_DISABLE(JacORBSubsystemConstants.INTEROP_INDIRECTION_ENCODING_DISABLE),
        INTEROP_STRICT_CHECK_ON_TC_CREATION(JacORBSubsystemConstants.INTEROP_STRICT_CHECK_ON_TC_CREATION),

        // attributes of the security element.
        SECURITY_SUPPORT_SSL(JacORBSubsystemConstants.SECURITY_SUPPORT_SSL),
        SECURITY_SECURITY_DOMAIN(JacORBSubsystemConstants.SECURITY_SECURITY_DOMAIN),
        SECURITY_ADD_COMPONENT_INTERCEPTOR(JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR),
        SECURITY_CLIENT_SUPPORTS(JacORBSubsystemConstants.SECURITY_CLIENT_SUPPORTS),
        SECURITY_CLIENT_REQUIRES(JacORBSubsystemConstants.SECURITY_CLIENT_REQUIRES),
        SECURITY_SERVER_SUPPORTS(JacORBSubsystemConstants.SECURITY_SERVER_SUPPORTS),
        SECURITY_SERVER_REQUIRES(JacORBSubsystemConstants.SECURITY_SERVER_REQUIRES),
        // if enabled the ORB service will configure JacORB to use the JBoss SSL socket factory classes by building the
        // appropriate properties.
        SECURITY_USE_DOMAIN_SF(JacORBSubsystemConstants.SECURITY_USE_DOMAIN_SF),
        SECURITY_USE_DOMAIN_SSF(JacORBSubsystemConstants.SECURITY_USE_DOMAIN_SSF),

        // attributes of the generic property element.
        PROP_KEY(JacORBSubsystemConstants.PROPERTY_KEY),
        PROP_VALUE(JacORBSubsystemConstants.PROPERTY_VALUE);

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
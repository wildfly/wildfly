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

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;

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
import static org.jboss.as.jacorb.JacORBAttribute.*;
import static org.jboss.as.jacorb.JacORBElement.*;

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

        final EnumSet<JacORBElement> encountered = EnumSet.noneOf(JacORBElement.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (JacORBNamespace.forUri(reader.getNamespaceURI())) {
                case JacORB_1_0: {
                    final JacORBElement element = JacORBElement.forName(reader.getLocalName());
                    // there can be multiple property elements.
                    if (!encountered.add(element) && element != PROPERTY_CONFIG) {
                        throw duplicateNamedElement(reader, element.getLocalName());
                    }
                    switch (element) {
                        case ORB_CONFIG: {
                            this.parseORBConfig(reader, subsystem.get(ORB_CONFIG.getLocalName()));
                            break;
                        }
                        case POA_CONFIG: {
                            this.parsePOAConfig(reader, subsystem.get(POA_CONFIG.getLocalName()));
                            break;
                        }
                        case INTEROP_CONFIG: {
                            this.parseInteropConfig(reader, subsystem.get(INTEROP_CONFIG.getLocalName()));
                            break;
                        }
                        case SECURITY_CONFIG: {
                            this.parseSecurityConfig(reader, subsystem.get(SECURITY_CONFIG.getLocalName()));
                            break;
                        }
                        case PROPERTY_CONFIG: {
                            this.parseGenericProperty(reader, subsystem.get(PROPERTY_CONFIG.getLocalName()));
                            break;
                        }
                        case INITIALIZERS_CONFIG: {
                            this.parseInitializersConfig(reader, subsystem.get(INITIALIZERS_CONFIG.getLocalName()));
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

        nodes.add(subsystem);
    }

    /**
     * <p>
     * Parses the {@code orb} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader    the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param orbConfig the {@code ModelNode} that will hold the parsed ORB configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseORBConfig(XMLExtendedStreamReader reader, ModelNode orbConfig) throws XMLStreamException {

        // parse the orb config attributes.
        EnumSet<JacORBAttribute> expectedAttributes = EnumSet.of(ORB_NAME, ORB_PRINT_VERSION, ORB_GIOP_MINOR_VERSION,
                ORB_USE_BOM, ORB_USE_IMR, ORB_CACHE_POA_NAMES, ORB_CACHE_TYPECODES);
        this.parseAttributes(reader, orbConfig, expectedAttributes, null);

        // parse the orb config elements.
        EnumSet<JacORBElement> foundElements = EnumSet.noneOf(JacORBElement.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (JacORBNamespace.forUri(reader.getNamespaceURI())) {
                case JacORB_1_0: {
                    final JacORBElement element = JacORBElement.forName(reader.getLocalName());
                    // check for duplicate elements.
                    if (!foundElements.add(element)) {
                        throw duplicateNamedElement(reader, element.getLocalName());
                    }
                    switch (element) {
                        case ORB_CONNECTION_CONFIG: {
                            // create a model node to hold the poa orb connection config attributes.
                            ModelNode subNode = orbConfig.get(ORB_CONNECTION_CONFIG.getLocalName());
                            // parse the orb connection config attributes.
                            EnumSet<JacORBAttribute> attributes = EnumSet.of(ORB_CONN_RETRIES, ORB_CONN_RETRY_INTERVAL,
                                    ORB_CONN_CLIENT_TIMEOUT, ORB_CONN_SERVER_TIMEOUT, ORB_CONN_MAX_SERVER_CONNECTIONS,
                                    ORB_CONN_MAX_MANAGED_BUF_SIZE, ORB_CONN_OUTBUF_SIZE, ORB_CONN_OUTBUF_CACHE_TIMEOUT);
                            this.parseAttributes(reader, subNode, attributes, null);
                            // the connection element doesn't have child elements.
                            requireNoContent(reader);
                            break;
                        }
                        case ORB_NAMING_CONFIG: {
                            // create a model node to hold the naming config attributes.
                            ModelNode subNode = orbConfig.get(ORB_NAMING_CONFIG.getLocalName());
                            // parse the naming config attributes.
                            EnumSet<JacORBAttribute> attributes = EnumSet.of(ORB_NAMING_ROOT_CONTEXT,
                                    ORB_NAMING_EXPORT_CORBALOC);
                            this.parseAttributes(reader, subNode, attributes, null);
                            // the naming element doesn't have child elements.
                            requireNoContent(reader);
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
     * Parses the {@code poa} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader    the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param poaConfig the {@code ModelNode} that will hold the parsed POA configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parsePOAConfig(XMLExtendedStreamReader reader, ModelNode poaConfig) throws XMLStreamException {

        // parse the poa config attributes.
        EnumSet<JacORBAttribute> expectedAttributes = EnumSet.of(POA_MONITORING, POA_QUEUE_WAIT, POA_QUEUE_MIN, POA_QUEUE_MAX);
        this.parseAttributes(reader, poaConfig, expectedAttributes, null);

        // parse the poa config elements.
        EnumSet<JacORBElement> foundElements = EnumSet.noneOf(JacORBElement.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (JacORBNamespace.forUri(reader.getNamespaceURI())) {
                case JacORB_1_0: {
                    final JacORBElement element = JacORBElement.forName(reader.getLocalName());
                    // check for duplicate elements.
                    if (!foundElements.add(element)) {
                        throw duplicateNamedElement(reader, element.getLocalName());
                    }
                    switch (element) {
                        case POA_REQUEST_PROC_CONFIG: {
                            // create a model node to hold the poa request-processors config attributes.
                            ModelNode subNode = poaConfig.get(POA_REQUEST_PROC_CONFIG.getLocalName());
                            // parse the poa request-processors config attributes.
                            EnumSet<JacORBAttribute> attributes =
                                    EnumSet.of(POA_REQUEST_PROC_POOL_SIZE, POA_REQUEST_PROC_MAX_THREADS);
                            this.parseAttributes(reader, subNode, attributes, EnumSet.copyOf(attributes));
                            // the request-processors element doesn't have child elements.
                            requireNoContent(reader);
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
     * Parses the {@code interop} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader        the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param interopConfig the {@code ModelNode} that will hold the parsed interoperability configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseInteropConfig(XMLExtendedStreamReader reader, ModelNode interopConfig) throws XMLStreamException {
        // parse all interop attributes.
        EnumSet<JacORBAttribute> expectedAttributes = EnumSet.of(INTEROP_SUN, INTEROP_COMET, INTEROP_CHUNK_RMI_VALUETYPES,
                INTEROP_LAX_BOOLEAN_ENCODING, INTEROP_INDIRECTION_ENCODING_DISABLE, INTEROP_STRICT_CHECK_ON_TC_CREATION);
        this.parseAttributes(reader, interopConfig, expectedAttributes, null);
        // the interop element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code security} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader         the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param securityConfig the {@code ModelNode} that will hold the parsed security configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseSecurityConfig(XMLExtendedStreamReader reader, ModelNode securityConfig) throws XMLStreamException {
        // parse all security attributes.
        EnumSet<JacORBAttribute> expectedAttributes = EnumSet.of(SECURITY_SUPPORT_SSL, SECURITY_ADD_COMPONENT_INTERCEPTOR,
                SECURITY_CLIENT_SUPPORTS, SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES,
                SECURITY_USE_DOMAIN_SF, SECURITY_USE_DOMAIN_SSF);
        this.parseAttributes(reader, securityConfig, expectedAttributes, null);
        // the security element doesn't have child elements.
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses a {@code property} element and adds the key/value pair to the specified {@code ModelNode}.
     * </p>
     *
     * @param reader        the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param orbProperties the {@code ModelNode} that contains all parsed ORB properties.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseGenericProperty(XMLExtendedStreamReader reader, ModelNode orbProperties) throws XMLStreamException {
        String key = null;
        String val = null;
        EnumSet<JacORBAttribute> required = EnumSet.of(JacORBAttribute.PROP_KEY, JacORBAttribute.PROP_VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final JacORBAttribute attribute = JacORBAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case PROP_KEY: {
                    key = value;
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

        orbProperties.add(key, val);
        requireNoContent(reader);
    }

    /**
     * <p>
     * Parses the {@code initializers} section of the JacORB subsystem configuration.
     * </p>
     *
     * @param reader     the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param initConfig the {@code ModelNode} that will hold the parsed initializers configuration.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseInitializersConfig(XMLExtendedStreamReader reader, ModelNode initConfig) throws XMLStreamException {
        requireNoAttributes(reader);
        initConfig.set(reader.getElementText().trim());
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
    private void parseAttributes(XMLExtendedStreamReader reader, ModelNode node, EnumSet<JacORBAttribute> expectedAttributes,
                                 EnumSet<JacORBAttribute> requiredAttributes) throws XMLStreamException {

        EnumSet<JacORBAttribute> parsedAttributes = EnumSet.noneOf(JacORBAttribute.class);
        if (requiredAttributes == null) {
            requiredAttributes = EnumSet.noneOf(JacORBAttribute.class);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final JacORBAttribute attribute = JacORBAttribute.forName(reader.getAttributeLocalName(i));
            // check for unexpected attributes.
            if (!expectedAttributes.contains(attribute))
                throw unexpectedAttribute(reader, i);
            // check for duplicate attributes.
            if (!parsedAttributes.add(attribute)) {
                throw duplicateAttribute(reader, attribute.getLocalName());
            }
            requiredAttributes.remove(attribute);
            node.get(attribute.getLocalName()).set(attrValue);
        }

        // throw an exception if a required attribute wasn't found.
        if (!requiredAttributes.isEmpty()) {
            throw missingRequired(reader, requiredAttributes);
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(JacORBNamespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        // write the orb configuration element.
        String orbConfig = ORB_CONFIG.getLocalName();
        if (node.hasDefined(orbConfig)) {
            this.writeORBConfig(writer, node.get(orbConfig));
        }
        // write the poa configuration element.
        String poaConfig = POA_CONFIG.getLocalName();
        if (node.hasDefined(poaConfig)) {
            this.writePOAConfig(writer, node.get(poaConfig));
        }
        // write the interop configuration element.
        String interopConfig = INTEROP_CONFIG.getLocalName();
        if (node.hasDefined(interopConfig)) {
            this.writeInteropConfig(writer, node.get(interopConfig));
        }
        // write the security configuration element.
        String securityConfig = SECURITY_CONFIG.getLocalName();
        if (node.hasDefined(securityConfig)) {
            this.writeSecurityConfig(writer, node.get(securityConfig));
        }
        // write all defined generic properties.
        String property = PROPERTY_CONFIG.getLocalName();
        if (node.hasDefined(property) && node.get(property).asInt() > 0) {
            this.writeGenericProperties(writer, node.get(property));
        }
        // write the ORB initializers if they have been defined.
        String initializers = INITIALIZERS_CONFIG.getLocalName();
        if (node.hasDefined(initializers)) {
            this.writeInitializersConfig(writer, node.get(initializers));
        }
        writer.writeEndElement(); // End of subsystem element
    }

    /**
     * <p>
     * Writes the {@code orb} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer    the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param orbConfig the {@code ModelNode} that contains the ORB configuration properties.
     * @throws XMLStreamException if an error occurs while writing the ORB configuration.
     */
    private void writeORBConfig(XMLExtendedStreamWriter writer, ModelNode orbConfig) throws XMLStreamException {
        // lets clone the incoming model node so we can change it.
        ModelNode clone = orbConfig.clone();
        ModelNode orbConnectionConfig = null;
        ModelNode orbNamingConfig = null;

        // check if the orb config includes connection and naming configs.
        String orbConnConfigName = ORB_CONNECTION_CONFIG.getLocalName();
        if (clone.hasDefined(orbConnConfigName)) {
            orbConnectionConfig = clone.get(orbConnConfigName);
            clone.remove(orbConnConfigName);
        }
        String orbNamingConfigName = ORB_NAMING_CONFIG.getLocalName();
        if (clone.hasDefined(orbNamingConfigName)) {
            orbNamingConfig = clone.get(orbNamingConfigName);
            clone.remove(orbNamingConfigName);
        }

        // if no sub configs were found, write an empty element with its attributes.
        if (orbConnectionConfig == null && orbNamingConfig == null) {
            writer.writeEmptyElement(ORB_CONFIG.getLocalName());
            this.writeAttributes(writer, clone);
        }

        // else write the orb config, its attributes and the sub configs.
        else {
            writer.writeStartElement(ORB_CONFIG.getLocalName());
            this.writeAttributes(writer, clone);
            if (orbConnectionConfig != null) {
                writer.writeEmptyElement(orbConnConfigName);
                this.writeAttributes(writer, orbConnectionConfig);
            }
            if (orbNamingConfig != null) {
                writer.writeEmptyElement(orbNamingConfigName);
                this.writeAttributes(writer, orbNamingConfig);
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
     * @param writer    the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param poaConfig the {@code ModelNode} that contains the POA configuration properties.
     * @throws XMLStreamException if an error occurs while writing the POA configuration.
     */
    private void writePOAConfig(XMLExtendedStreamWriter writer, ModelNode poaConfig) throws XMLStreamException {
        ModelNode clone = poaConfig.clone();
        ModelNode poaRequestProcessorsConfig = null;

        String poaRequestProcessorsConfigName = POA_REQUEST_PROC_CONFIG.getLocalName();
        if (clone.hasDefined(poaRequestProcessorsConfigName)) {
            poaRequestProcessorsConfig = clone.get(poaRequestProcessorsConfigName);
            clone.remove(poaRequestProcessorsConfigName);
        }

        // if no sub configs were found, write an empty poa element with its attributes.
        if (poaRequestProcessorsConfig == null) {
            writer.writeEmptyElement(POA_CONFIG.getLocalName());
            this.writeAttributes(writer, clone);
        } else {
            writer.writeStartElement(POA_CONFIG.getLocalName());
            this.writeAttributes(writer, clone);
            writer.writeEmptyElement(poaRequestProcessorsConfigName);
            this.writeAttributes(writer, poaRequestProcessorsConfig);
            writer.writeEndElement();
        }
    }

    /**
     * <p>
     * Writes the {@code interop} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer        the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param interopConfig the {@code ModelNode} that contains the interoperability configuration properties.
     * @throws XMLStreamException if an error occurs while writing the interop configuration.
     */
    private void writeInteropConfig(XMLExtendedStreamWriter writer, ModelNode interopConfig) throws XMLStreamException {
        writer.writeEmptyElement(INTEROP_CONFIG.getLocalName());
        this.writeAttributes(writer, interopConfig);
    }

    /**
     * <p>
     * Writes the {@code security} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer         the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param securityConfig the {@code ModelNode} that contains the security configuration properties.
     * @throws XMLStreamException if an error occurs while writing the security configuration.
     */
    private void writeSecurityConfig(XMLExtendedStreamWriter writer, ModelNode securityConfig) throws XMLStreamException {
        writer.writeEmptyElement(SECURITY_CONFIG.getLocalName());
        this.writeAttributes(writer, securityConfig);
    }

    /**
     * <p>
     * Writes a {@code property} element for each generic property contained in the specified {@code ModelNode}.
     * </p>
     *
     * @param writer    the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param modelNode the {@code ModelNode} that contains all properties to be written.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while writing the property elements.
     */
    private void writeGenericProperties(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        for (Property prop : modelNode.asPropertyList()) {
            writer.writeEmptyElement(JacORBElement.PROPERTY_CONFIG.getLocalName());
            writer.writeAttribute(JacORBAttribute.PROP_KEY.getLocalName(), prop.getName());
            writer.writeAttribute(JacORBAttribute.PROP_VALUE.getLocalName(), prop.getValue().asString());
        }
    }

    /**
     * <p>
     * Writes the {@code initializers} section of the JacORB subsystem configuration using the contents of the provided
     * {@code ModelNode}.
     * </p>
     *
     * @param writer     the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param initConfig the {@code ModelNode} that contains the initializers configuration properties.
     * @throws XMLStreamException if an error occurs while writing the initializers configuration.
     */
    private void writeInitializersConfig(XMLExtendedStreamWriter writer, ModelNode initConfig) throws XMLStreamException {
        writer.writeStartElement(INITIALIZERS_CONFIG.getLocalName());
        writer.writeCharacters(initConfig.asString());
        writer.writeEndElement();
    }

    /**
     * <p>
     * Writes the attributes contained in the specified {@code ModelNode} to the current element.
     * </p>
     *
     * @param writer     the {@code XMLExtendedStreamWriter} used to write the configuration XML.
     * @param attributes the {@code ModelNode} that contains the attributes to be written.
     * @throws XMLStreamException if an error occurs while writing the attributes to the current element.
     */
    private void writeAttributes(XMLExtendedStreamWriter writer, ModelNode attributes) throws XMLStreamException {
        if (attributes.asInt() > 0) {
            for (Property property : attributes.asPropertyList()) {
                writer.writeAttribute(property.getName(), property.getValue().asString());
            }
        }
    }

}
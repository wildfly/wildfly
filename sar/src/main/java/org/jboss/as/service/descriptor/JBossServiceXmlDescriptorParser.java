/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service.descriptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.service.logging.SarLogger;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Tomasz Adamski
 */
public final class JBossServiceXmlDescriptorParser implements XMLElementReader<ParseResult<JBossServiceXmlDescriptor>>, XMLStreamConstants {

    private enum Namespace {
        UNKNOWN(null),
        NONE(""),
        SERVICE_7_0("urn:jboss:service:7.0"),
        ;
        private final String namespace;

        private static final Map<String, Namespace> NS_MAP = new HashMap<String, Namespace>();

        static {
            for (Namespace namespace : Namespace.values()) {
                NS_MAP.put(namespace.namespace, namespace);
            }
        }

        private Namespace(String namespace) {
            this.namespace = namespace;
        }

        public static Namespace of(final String uri) {
            if (uri == null) return NONE;
            final Namespace namespace = NS_MAP.get(uri);
            return namespace == null ? UNKNOWN : namespace;
        }
    }

    public static final String NAMESPACE = "urn:jboss:service:7.0";

    private enum Element {
        UNKNOWN(null),
        MBEAN("mbean"),
        CONSTRUCTOR("constructor"),
        ARG("arg"),
        ATTRIBUTE("attribute"),
        INJECT("inject"),
        VALUE_FACTORY("value-factory"),
        PARAMETER("parameter"),
        DEPENDS("depends"),
        DEPENDS_LIST("depends-list"),
        DEPENDS_LIST_ELEMENT("depends-list-element"),
        ALIAS("alias"),
        ANNOTATION("annotation"),
        ;

        private final String localName;

        private static final Map<String, Element> NAME_MAP = new HashMap<String, Element>();

        static {
            for (Element element : Element.values()) {
                NAME_MAP.put(element.localName, element);
            }
        }

        private Element(final String localName) {
            this.localName = localName;
        }

        static Element of(final String localName) {
            final Element element = NAME_MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }
    }

    private enum Attribute {
        MODE(new QName("mode")),
        NAME(new QName("name")),
        CODE(new QName("code")),
        TYPE(new QName("type")),
        VALUE(new QName("value")),
        TRIM(new QName("trim")),
        REPLACE(new QName("replace")),
        BEAN(new QName("bean")),
        PROPERTY(new QName("property")),
        CLASS(new QName("class")),
        METHOD(new QName("method")),

        OPTIONAL_ATTRIBUTE_NAME(new QName("optional-attribute-name")),
        PROXY_TYPE(new QName("proxy-type")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Attribute> QNAME_MAP = new HashMap<QName, Attribute>();

        static {
            for(Attribute attribute : Attribute.values()) {
                QNAME_MAP.put(attribute.qName, attribute);
            }
        }

        private Attribute(final QName qName) {
            this.qName = qName;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = QNAME_MAP.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    private final PropertyReplacer propertyReplacer;

    public JBossServiceXmlDescriptorParser(final PropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<JBossServiceXmlDescriptor> value) throws XMLStreamException {
        final JBossServiceXmlDescriptor serviceXmlDescriptor = new JBossServiceXmlDescriptor();
        final List<JBossServiceConfig> serviceConfigs = new ArrayList<JBossServiceConfig>();
        serviceXmlDescriptor.setServiceConfigs(serviceConfigs);
        value.setResult(serviceXmlDescriptor);

        final int count = reader.getAttributeCount();
        for(int i = 0; i < count; i++) {
            final QName attributeName = reader.getAttributeName(i);
            final Attribute attribute = Attribute.of(attributeName);
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case MODE:
                    serviceXmlDescriptor.setControllerMode(JBossServiceXmlDescriptor.ControllerMode.of(attributeValue));
                    break;
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case COMMENT:
                    break;
                case END_ELEMENT:
                    return;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch (Element.of(reader.getLocalName())) {
                        case MBEAN:
                            serviceConfigs.add(parseMBean(reader));
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
            }
        }
    }

    private JBossServiceConfig parseMBean(final XMLExtendedStreamReader reader) throws XMLStreamException  {
        // Handle Attributes
        final JBossServiceConfig serviceConfig = new JBossServiceConfig();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.CODE);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch(attribute) {
                case NAME:
                    serviceConfig.setName(attributeValue);
                    break;
                case CODE:
                    serviceConfig.setCode(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        final List<JBossServiceDependencyConfig> dependencyConfigs = new ArrayList<JBossServiceDependencyConfig>();
        final List<JBossServiceDependencyListConfig> dependencyListConfigs = new ArrayList<JBossServiceDependencyListConfig>();
        final List<JBossServiceAttributeConfig> attributes = new ArrayList<JBossServiceAttributeConfig>();
        final List<String> aliases = new ArrayList<String>();
        final List<String> annotations = new ArrayList<String>();

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT:
                    serviceConfig.setDependencyConfigs(dependencyConfigs.toArray(new JBossServiceDependencyConfig[dependencyConfigs.size()]));
                    serviceConfig.setDependencyConfigLists(dependencyListConfigs.toArray(new JBossServiceDependencyListConfig[dependencyListConfigs.size()]));
                    serviceConfig.setAliases(aliases.toArray(new String[aliases.size()]));
                    serviceConfig.setAnnotations(annotations.toArray(new String[annotations.size()]));
                    serviceConfig.setAttributeConfigs(attributes.toArray(new JBossServiceAttributeConfig[attributes.size()]));

                    return serviceConfig;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch(Element.of(reader.getLocalName())) {
                        case CONSTRUCTOR:
                            serviceConfig.setConstructorConfig(parseConstructor(reader));
                            break;
                        case DEPENDS:
                            dependencyConfigs.add(parseDepends(reader));
                            break;
                        case DEPENDS_LIST:
                            dependencyListConfigs.add(parseDependsList(reader));
                            break;
                        case ALIAS:
                            aliases.add(parseTextElement(reader));
                            break;
                        case ANNOTATION:
                            annotations.add(parseTextElement(reader));
                            break;
                        case ATTRIBUTE:
                            attributes.add(parseAttribute(reader));
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceConstructorConfig parseConstructor(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceConstructorConfig constructorConfig = new JBossServiceConstructorConfig();
        final List<JBossServiceConstructorConfig.Argument> arguments = new ArrayList<JBossServiceConstructorConfig.Argument>();

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT:
                    constructorConfig.setArguments(arguments.toArray(new JBossServiceConstructorConfig.Argument[arguments.size()]));
                    return constructorConfig;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch(Element.of(reader.getLocalName())) {
                        case ARG:
                            arguments.add(parseArgument(reader));
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceConstructorConfig.Argument parseArgument(XMLExtendedStreamReader reader) throws XMLStreamException {
        String type = null;
        String value = null;
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.TYPE, Attribute.VALUE);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch(attribute) {
                case TYPE:
                    type = attributeValue;
                    break;
                case VALUE:
                    value = attributeValue;
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        reader.discardRemainder();
        return new JBossServiceConstructorConfig.Argument(type, value);
    }

    private JBossServiceAttributeConfig parseAttribute(XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceAttributeConfig attributeConfig = new JBossServiceAttributeConfig();
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch(attribute) {
                case NAME:
                    attributeConfig.setName(attributeValue);
                    break;
                case TRIM:
                    attributeConfig.setTrim(Boolean.parseBoolean(attributeValue));
                    break;
                case REPLACE:
                    attributeConfig.setReplace(Boolean.parseBoolean(attributeValue));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        final StringBuilder valueBuilder = new StringBuilder();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    attributeConfig.setValue(propertyReplacer.replaceProperties(valueBuilder.toString().trim()));
                    return attributeConfig;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch(Element.of(reader.getLocalName())) {
                        case INJECT:
                            attributeConfig.setInject(parseInject(reader));
                            break;
                        case VALUE_FACTORY:
                            attributeConfig.setValueFactory(parseValueFactory(reader));
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                case CHARACTERS:
                    valueBuilder.append(reader.getText());
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceAttributeConfig.Inject parseInject(XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceAttributeConfig.Inject injectConfig = new JBossServiceAttributeConfig.Inject();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.BEAN);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case BEAN:
                    injectConfig.setBeanName(attributeValue);
                    break;
                case PROPERTY:
                    injectConfig.setPropertyName(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        reader.discardRemainder();
        return injectConfig;
    }

    private JBossServiceAttributeConfig.ValueFactory parseValueFactory(XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceAttributeConfig.ValueFactory valueFactory = new JBossServiceAttributeConfig.ValueFactory();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.BEAN, Attribute.METHOD);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case BEAN:
                    valueFactory.setBeanName(attributeValue);
                    break;
                case METHOD:
                    valueFactory.setMethodName(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        final List<JBossServiceAttributeConfig.ValueFactoryParameter> parameters = new ArrayList<JBossServiceAttributeConfig.ValueFactoryParameter>();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    valueFactory.setParameters(parameters.toArray(new JBossServiceAttributeConfig.ValueFactoryParameter[parameters.size()]));
                    return valueFactory;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch(Element.of(reader.getLocalName())) {
                        case PARAMETER:
                            parameters.add(parseValueFactoryParameter(reader));
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceAttributeConfig.ValueFactoryParameter parseValueFactoryParameter(XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceAttributeConfig.ValueFactoryParameter parameterConfig = new JBossServiceAttributeConfig.ValueFactoryParameter();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.CLASS);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case CLASS:
                    parameterConfig.setType(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return parameterConfig;
                case CHARACTERS:
                    parameterConfig.setValue(reader.getText());
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceDependencyConfig parseDepends(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceDependencyConfig dependencyConfig = new JBossServiceDependencyConfig();
        final int count = reader.getAttributeCount();
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case OPTIONAL_ATTRIBUTE_NAME:
                    dependencyConfig.setOptionalAttributeName(attributeValue);
                    break;
                case PROXY_TYPE:
                    dependencyConfig.setProxyType(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        parseDependency(reader, dependencyConfig);
        return dependencyConfig;
    }

    private JBossServiceDependencyListConfig parseDependsList(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceDependencyListConfig dependencyListConfig  = new JBossServiceDependencyListConfig();
        final List<JBossServiceDependencyConfig> dependencyConfigs = new ArrayList<JBossServiceDependencyConfig>();
        String optionalAttributeName = null;
        final int count = reader.getAttributeCount();
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case OPTIONAL_ATTRIBUTE_NAME:
                    optionalAttributeName = attributeValue;
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }

        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    dependencyListConfig.setOptionalAttributeName(optionalAttributeName);
                    dependencyListConfig.setDependencyConfigs(dependencyConfigs.toArray(new JBossServiceDependencyConfig[dependencyConfigs.size()]));
                    return dependencyListConfig;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch(Element.of(reader.getLocalName())) {
                        case DEPENDS_LIST_ELEMENT:
                            final JBossServiceDependencyConfig dependencyConfig = new JBossServiceDependencyConfig();
                            parseDependency(reader, dependencyConfig);
                            dependencyConfigs.add(dependencyConfig);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private void parseDependency(final XMLExtendedStreamReader reader, final JBossServiceDependencyConfig dependencyConfig) throws XMLStreamException {
        final StringBuilder nameBuilder = new StringBuilder();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    dependencyConfig.setDependencyName(nameBuilder.toString().trim());
                    return;
                case START_ELEMENT:
                    switch (Namespace.of(reader.getNamespaceURI())) {
                        case NONE:
                        case SERVICE_7_0: {
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    switch(Element.of(reader.getLocalName())) {
                        case MBEAN:
                            dependencyConfig.setServiceConfig(parseMBean(reader));
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                case CHARACTERS:
                    nameBuilder.append(reader.getText());
                    break;
            }
        }
    }

    private String parseTextElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final StringBuilder valueBuilder = new StringBuilder();
        while (reader.hasNext()) {
            switch(reader.next()) {
                case END_ELEMENT:
                    return valueBuilder.toString().trim();
                case CHARACTERS:
                    valueBuilder.append(reader.getText());
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE: kind = "attribute"; break;
            case XMLStreamConstants.CDATA: kind = "cdata"; break;
            case XMLStreamConstants.CHARACTERS: kind = "characters"; break;
            case XMLStreamConstants.COMMENT: kind = "comment"; break;
            case XMLStreamConstants.DTD: kind = "dtd"; break;
            case XMLStreamConstants.END_DOCUMENT: kind = "document end"; break;
            case XMLStreamConstants.END_ELEMENT: kind = "element end"; break;
            case XMLStreamConstants.ENTITY_DECLARATION: kind = "entity decl"; break;
            case XMLStreamConstants.ENTITY_REFERENCE: kind = "entity ref"; break;
            case XMLStreamConstants.NAMESPACE: kind = "namespace"; break;
            case XMLStreamConstants.NOTATION_DECLARATION: kind = "notation decl"; break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION: kind = "processing instruction"; break;
            case XMLStreamConstants.SPACE: kind = "whitespace"; break;
            case XMLStreamConstants.START_DOCUMENT: kind = "document start"; break;
            case XMLStreamConstants.START_ELEMENT: kind = "element start"; break;
            default: kind = "unknown"; break;
        }
        return new XMLStreamException(SarLogger.ROOT_LOGGER.unexpectedContent(kind, reader.getName(), reader.getText()), reader.getLocation());
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder(SarLogger.ROOT_LOGGER.missingRequiredAttributes());
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException(b.toString(), location);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLContainer;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.Feature;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;

/**
 * Encapsulates an XML container of content for a resource.
 * @param <C> the writer context
 */
public interface ResourceXMLContainer extends XMLContainer<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder<T extends ResourceXMLContainer, B extends Builder<T, B>> extends XMLContainer.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, T, B>, AttributeDefinitionXMLConfiguration.Configurator<B> {
        /**
         * Adds the specified attribute to this element.
         * @param attribute a resource attribute definition
         * @return a reference to this builder
         */
        B addAttribute(AttributeDefinition attribute);

        /**
         * Adds the specified attributes to this element.
         * @param attributes a collection resource attribute definition
         * @return a reference to this builder
         */
        B addAttributes(Iterable<AttributeDefinition> attributes);

        /**
         * Adds an attribute with the specified name to this element that should be allowed, but ignored during read/write.
         * @param localName the local name of the ignored attribute
         * @return a reference to this builder
         */
        B ignoreAttribute(String localName);

        /**
         * Adds an attribute with the specified name to this element that should be allowed, but ignored during read/write.
         * @param localName the qualified name of the ignored attribute
         * @return a reference to this builder
         */
        B ignoreAttribute(QName name);
    }

    abstract class AbstractBuilder<T extends ResourceXMLContainer, B extends Builder<T, B>> extends XMLContainer.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, T, B> implements Builder<T, B>, FeatureFilter, QNameResolver {
        private static final AttributeParser IGNORED_PARSER = new AttributeParser() {
            @Override
            public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
                ClusteringLogger.ROOT_LOGGER.attributeIgnored(reader.getName(), new QName(reader.getNamespaceURI(), attribute.getXmlName()));
            }
        };
        private static final AttributeMarshaller NO_OP_MARSHALLER = new AttributeMarshaller() {
            @Override
            public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                // Do nothing
            }
        };
        static SimpleAttributeDefinitionBuilder ignoredAttributeDefinitionBuilder(String name) {
            return new SimpleAttributeDefinitionBuilder(name, ModelType.STRING).setRequired(false).setAttributeMarshaller(NO_OP_MARSHALLER);
        }

        private final FeatureFilter filter;
        private final QNameResolver resolver;
        private final List<AttributeDefinition> attributes = new LinkedList<>();
        private volatile AttributeDefinitionXMLConfiguration configuration;

        protected AbstractBuilder(FeatureFilter filter, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            this.filter = filter;
            this.resolver = resolver;
            this.configuration = configuration;
        }

        @Override
        public QName resolve(String localName) {
            return this.resolver.resolve(localName);
        }

        @Override
        public <F extends Feature> boolean enables(F feature) {
            return this.filter.enables(feature);
        }

        @Override
        public B addAttributes(Iterable<AttributeDefinition> attributes) {
            for (AttributeDefinition attribute : attributes) {
                if (this.filter.enables(attribute)) {
                    this.attributes.add(attribute);
                }
            }
            return this.builder();
        }

        @Override
        public B ignoreAttribute(String localName) {
            return this.addAttribute(ignoredAttributeDefinitionBuilder(localName).setAttributeParser(IGNORED_PARSER).build());
        }

        @Override
        public B addAttribute(AttributeDefinition attribute) {
            return this.addAttributes(List.of(attribute));
        }

        @Override
        public B ignoreAttribute(QName name) {
            return this.ignoreAttribute(name.getLocalPart());
        }

        @Override
        public B withParsers(Map<AttributeDefinition, AttributeParser> parsers) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public AttributeParser getParser(AttributeDefinition attribute) {
                    AttributeParser parser = parsers.get(attribute);
                    return (parser != null) ? parser : super.getParser(attribute);
                }
            };
            return this.builder();
        }

        @Override
        public B withMarshallers(Map<AttributeDefinition, AttributeMarshaller> marshallers) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public AttributeMarshaller getMarshaller(AttributeDefinition attribute) {
                    AttributeMarshaller marshaller = marshallers.get(attribute);
                    return (marshaller != null) ? marshaller : super.getMarshaller(attribute);
                }
            };
            return this.builder();
        }

        Collection<AttributeDefinition> getAttributes() {
            return this.attributes;
        }

        AttributeDefinitionXMLConfiguration getConfiguration() {
            return this.configuration;
        }
    }

    class ResourceXMLContainerReader implements org.jboss.staxmapper.XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> {
        private final QName name;
        private final XMLElementReader<ModelNode> attributesReader;
        private final XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content;

        ResourceXMLContainerReader(QName name, XMLElementReader<ModelNode> attributesReader, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content) {
            this.name = name;
            this.attributesReader = attributesReader;
            this.content = content;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
            // Validate entry criteria
            Assert.assertTrue(reader.isStartElement());
            // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
            if (!reader.getName().equals(this.name) && !reader.getLocalName().equals(this.name.getLocalPart())) {
                throw ParseUtils.unexpectedElement(reader, Set.of(this.name.toString()));
            }

            PathAddress operationKey = context.getKey();
            Map<PathAddress, ModelNode> operations = context.getValue();
            ModelNode operation = operations.get(operationKey);
            this.attributesReader.readElement(reader, operation);

            this.content.readContent(reader, context);
            // Validate exit criteria
            // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
            if (!reader.isEndElement() || (!reader.getName().equals(this.name) && !reader.getLocalName().equals(this.name.getLocalPart()))) {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    class ResourceXMLContainerWriter<C> implements XMLContentWriter<C> {
        private final QName name;
        private final XMLContentWriter<C> attributesWriter;
        private final Function<C, ModelNode> model;
        private final XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> childContent;

        ResourceXMLContainerWriter(QName name, XMLContentWriter<C> attributesWriter, Function<C, ModelNode> model, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> childContent) {
            this.name = name;
            this.attributesWriter = attributesWriter;
            this.model = model;
            this.childContent = childContent;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, C content) throws XMLStreamException {
            String namespaceURI = this.name.getNamespaceURI();
            // If namespace is not yet bound to any prefix, bind it
            if (writer.getNamespaceContext().getPrefix(namespaceURI) == null) {
                writer.writeStartElement(this.name.getLocalPart());
                // Bind and write namespace
                if (namespaceURI != XMLConstants.NULL_NS_URI) { // For PersistentResourceXMLDescription compatibility
                    writer.setPrefix(this.name.getPrefix(), namespaceURI);
                    writer.writeNamespace(this.name.getPrefix(), namespaceURI);
                }
            } else {
                writer.writeStartElement(namespaceURI, this.name.getLocalPart());
            }
            this.attributesWriter.writeContent(writer, content);

            this.childContent.writeContent(writer, this.model.apply(content));

            writer.writeEndElement();
        }

        @Override
        public boolean isEmpty(C content) {
            return this.attributesWriter.isEmpty(content) && this.childContent.isEmpty(this.model.apply(content));
        }
    }

    static final XMLElementReader<ModelNode> EMPTY_READER = new XMLElementReader<>() {
        @Override
        public void readElement(XMLExtendedStreamReader reader, ModelNode value) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
        }
    };

    class ResourceAttributesXMLContentReader implements XMLElementReader<ModelNode> {
        private static final Comparator<QName> COMPARATOR = Comparator.comparing(QName::getLocalPart);
        private final Map<QName, AttributeDefinition> attributes;
        private final AttributeDefinitionXMLConfiguration configuration;

        ResourceAttributesXMLContentReader(Collection<AttributeDefinition> attributes, AttributeDefinitionXMLConfiguration configuration) {
            this.attributes = attributes.isEmpty() ? Map.of() : new TreeMap<>(COMPARATOR);
            this.configuration = configuration;
            // Collect only those attributes that will parse as an XML attribute
            for (AttributeDefinition attribute : attributes) {
                if (!configuration.getParser(attribute).isParseAsElement()) {
                    this.attributes.put(configuration.getName(attribute), attribute);
                }
            }
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            Set<QName> distinctAttributes = new TreeSet<>(COMPARATOR);
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                QName name = reader.getAttributeName(i);
                String localName = name.getLocalPart();
                if (!distinctAttributes.add(name)) {
                    throw ParseUtils.duplicateAttribute(reader, localName);
                }
                if (name.getNamespaceURI().equals(XMLConstants.NULL_NS_URI)) {
                    // Inherit namespace of element, if unspecified
                    name = new QName(reader.getNamespaceURI(), localName);
                }
                AttributeDefinition attribute = this.attributes.get(name);
                if (attribute == null) {
                    // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
                    attribute = this.attributes.get(new QName(localName));
                    if (attribute == null) {
                        throw ParseUtils.unexpectedAttribute(reader, i, this.attributes.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }
                }
                this.configuration.getParser(attribute).parseAndSetParameter(attribute, reader.getAttributeValue(i), operation, reader);
            }
        }
    }

    static final XMLContentWriter<ModelNode> EMPTY_WRITER = XMLContentWriter.empty();

    class ResourceAttributesXMLContentWriter implements XMLContentWriter<ModelNode>, Predicate<AttributeDefinition> {
        private final Collection<AttributeDefinition> attributes;
        private final AttributeDefinitionXMLConfiguration configuration;

        ResourceAttributesXMLContentWriter(Collection<AttributeDefinition> attributes, AttributeDefinitionXMLConfiguration configuration) {
            this.attributes = attributes;
            this.configuration = configuration;
        }

        @Override
        public boolean test(AttributeDefinition attribute) {
            return !this.configuration.getMarshaller(attribute).isMarshallableAsElement();
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
            for (AttributeDefinition attribute : this.attributes) {
                if (this.test(attribute)) {
                    this.configuration.getMarshaller(attribute).marshallAsAttribute(attribute, model, true, writer);
                }
            }
        }

        @Override
        public boolean isEmpty(ModelNode model) {
            return this.attributes.stream().filter(this).map(AttributeDefinition::getName).noneMatch(model::hasDefined);
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Encapsulates an XML container of content for a resource.
 * @param <C> the writer context
 */
public interface ResourceXMLContainer extends XMLContainer<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder<T extends ResourceXMLContainer, B extends Builder<T, B>> extends XMLContainer.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, T, B>, AttributeDefinitionXMLConfiguration.Configurator<B> {
        /**
         * Adds the specified attribute to this element, if it will parse and/or marshal as an xs:attribute.
         * @param attribute a resource attribute definition
         * @return a reference to this builder
         */
        B addAttribute(AttributeDefinition attribute);

        /**
         * Adds the specified attributes, that will parse and/or marshal as an xs:attribute, to this element.
         * @param attributes a collection resource attribute definition
         * @return a reference to this builder
         */
        B addAttributes(Iterable<? extends AttributeDefinition> attributes);

        /**
         * Specifies a set of attribute local names that should be allowed, but ignored during parsing.
         * @param localNames a set of ignored attribute local names
         * @return a reference to this builder
         */
        B ignoreAttributeLocalNames(Set<String> localNames);

        /**
         * Specifies a set of attribute local names that should be allowed, but ignored during parsing.
         * @param localNames a set of ignored attribute local names
         * @return a reference to this builder
         */
        B ignoreAttributeNames(Set<QName> names);
    }

    abstract class AbstractBuilder<T extends ResourceXMLContainer, B extends Builder<T, B>> extends XMLContainer.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, T, B> implements Builder<T, B>, FeatureRegistry, QNameResolver {
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
        static SimpleAttributeDefinitionBuilder ignoredAttributeDefinitionBuilder(String localName) {
            return new SimpleAttributeDefinitionBuilder(localName, ModelType.STRING).setRequired(false).setAttributeParser(IGNORED_PARSER).setAttributeMarshaller(NO_OP_MARSHALLER);
        }

        private final FeatureRegistry registry;
        private final QNameResolver resolver;
        private final List<AttributeDefinition> attributes = new LinkedList<>();
        private volatile AttributeDefinitionXMLConfiguration configuration;

        protected AbstractBuilder(FeatureRegistry registry, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            this.registry = registry;
            this.resolver = resolver;
            this.configuration = configuration;
        }

        @Override
        public QName resolve(String localName) {
            return this.resolver.resolve(localName);
        }

        @Override
        public Stability getStability() {
            return this.registry.getStability();
        }

        @Override
        public B addAttributes(Iterable<? extends AttributeDefinition> attributes) {
            for (AttributeDefinition attribute : attributes) {
                this.addAttribute(attribute);
            }
            return this.builder();
        }

        @Override
        public B addAttribute(AttributeDefinition attribute) {
            if (this.enables(attribute)) {
                this.attributes.add(attribute);
            }
            return this.builder();
        }

        @Override
        public B ignoreAttributeLocalNames(Set<String> localNames) {
            return this.ignoreAttributes(localNames.stream());
        }

        @Override
        public B ignoreAttributeNames(Set<QName> names) {
            return this.ignoreAttributes(names.stream().map(QName::getLocalPart));
        }

        private B ignoreAttributes(Stream<String> localNames) {
            localNames.map(AbstractBuilder::ignoredAttributeDefinitionBuilder).map(SimpleAttributeDefinitionBuilder::build).forEach(this::addAttribute);
            return this.builder();
        }

        @Override
        public B withLocalNames(Map<AttributeDefinition, String> localNames) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public QName getName(AttributeDefinition attribute) {
                    String localName = localNames.get(attribute);
                    return (localName != null) ? this.resolve(localName) : super.getName(attribute);
                }
            };
            return this.builder();
        }

        @Override
        public B withNames(Map<AttributeDefinition, QName> names) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public QName getName(AttributeDefinition attribute) {
                    QName name = names.get(attribute);
                    return (name != null) ? name : super.getName(attribute);
                }
            };
            return this.builder();
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
        private final XMLElementReader<ModelNode> attributesReader;
        private final XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content;

        ResourceXMLContainerReader(XMLElementReader<ModelNode> attributesReader, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content) {
            this.attributesReader = attributesReader;
            this.content = content;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
            PathAddress operationKey = context.getKey();
            Map<PathAddress, ModelNode> operations = context.getValue();
            ModelNode operation = operations.get(operationKey);
            this.attributesReader.readElement(reader, operation);

            this.content.readContent(reader, context);
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
            writer.writeStartElement(namespaceURI, this.name.getLocalPart());

            // If namespace is not yet bound to any prefix, bind it
            if (writer.getNamespaceContext().getPrefix(namespaceURI) == null) {
                writer.setPrefix(this.name.getPrefix(), namespaceURI);
                writer.writeNamespace(this.name.getPrefix(), namespaceURI);
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
        private final Set<QName> requiredAttributes;
        private final Map<QName, AttributeDefinition> attributes;
        private final AttributeDefinitionXMLConfiguration configuration;

        ResourceAttributesXMLContentReader(Collection<AttributeDefinition> attributes, AttributeDefinitionXMLConfiguration configuration) {
            this.attributes = attributes.isEmpty() ? Map.of() : new TreeMap<>(QNameResolver.COMPARATOR);
            this.requiredAttributes = attributes.isEmpty() ? Set.of() : new TreeSet<>(QNameResolver.COMPARATOR);
            this.configuration = configuration;
            // Collect only those attributes that will parse as an XML attribute
            for (AttributeDefinition attribute : attributes) {
                if (!configuration.getParser(attribute).isParseAsElement()) {
                    QName name = configuration.getName(attribute);
                    this.attributes.put(name, attribute);
                    if (attribute.isRequired()) {
                        this.requiredAttributes.add(name);
                    }
                }
            }
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            Set<QName> distinctAttributes = new TreeSet<>(QNameResolver.COMPARATOR);
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                QName name = reader.getAttributeName(i);
                if (name.getNamespaceURI().equals(XMLConstants.NULL_NS_URI) && !reader.getName().getNamespaceURI().equals(XMLConstants.NULL_NS_URI)) {
                    // Inherit namespace of element, if unspecified
                    name = new QName(reader.getName().getNamespaceURI(), name.getLocalPart());
                }
                if (!distinctAttributes.add(name)) {
                    throw ParseUtils.duplicateAttribute(reader, name.getLocalPart());
                }
                AttributeDefinition attribute = this.attributes.get(name);
                if (attribute == null) {
                    throw ParseUtils.unexpectedAttribute(reader, i, this.attributes.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                }
                this.configuration.getParser(attribute).parseAndSetParameter(attribute, reader.getAttributeValue(i), operation, reader);
            }
            if (!distinctAttributes.containsAll(this.requiredAttributes)) {
                Set<QName> required = new TreeSet<>(QNameResolver.COMPARATOR);
                required.addAll(this.requiredAttributes);
                required.removeAll(distinctAttributes);
                throw ParseUtils.missingRequired(reader, required);
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.clustering.controller.SubsystemResourceDescription;
import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLChoice;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentReader;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;
import org.wildfly.common.function.Functions;

/**
 * An XML element for a resource.
 * @author Paul Ferraro
 */
public interface ResourceXMLElement extends ResourceRegistration, ResourceXMLChoice, XMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    @Override
    default Set<PathElement> getPathElements() {
        return Set.of(this.getPathElement());
    }

    /**
     * A builder of a {@link ResourceXMLElement}.
     */
    interface Builder extends QNameResolver {

        /**
         * A factory that creates a builder of a {@link ResourceXMLElement}.
         */
        interface Factory extends QNameResolver {
            /**
             * Creates a new {@link ResourceXMLElement} builder for the specified subsystem resource description.
             * @param description a subsystem resource description
             * @return a new {@link ResourceXMLElement} builder
             */
            Builder createBuilder(SubsystemResourceDescription description);

            /**
             * Creates a new {@link ResourceXMLElement} builder for the specified resource description.
             * @param description a resource description
             * @return a new {@link ResourceXMLElement} builder
             */
            Builder createBuilder(ResourceDescription description);

            /**
             * Returns a factory for creating {@link ResourceXMLElement} builder instances for the specified subsystem schema.
             * @param <S> the schema type
             * @param schema a subsystem schema
             * @return a factory for creating {@link ResourceXMLElement} builder instances
             */
            static <S extends SubsystemSchema<S>> Factory newInstance(S schema) {
                return new Factory() {
                    @Override
                    public Builder createBuilder(SubsystemResourceDescription description) {
                        return new DefaultBuilder(schema, description, this).withElementLocalName(ResourceXMLElementLocalName.KEY);
                    }

                    @Override
                    public Builder createBuilder(ResourceDescription description) {
                        return new DefaultBuilder(schema, description, this);
                    }

                    @Override
                    public QName resolveQName(String localName) {
                        return new QName(schema.getNamespace().getUri(), localName);
                    }
                };
            }
        }

        /**
         * Indicates that this resource is required to be present.
         * @return a reference to this builder.
         */
        Builder require();

        /**
         * Overrides the local name of the attribute used to create the path for this resource.
         * Defaults to {@value ModelDescriptionConstants#NAME} if unspecified.
         * @param localName a attribute local name.
         * @return a reference to this builder.
         */
        default Builder withPathValueAttributeLocalName(String localName) {
            return this.withPathValueAttributeName(this.resolveQName(localName));
        }

        /**
         * Overrides the local name of the attribute used to create the path for this resource.
         * Defaults to {@value ModelDescriptionConstants#NAME} if unspecified.
         * @param name a attribute local name.
         * @return a reference to this builder.
         */
        Builder withPathValueAttributeName(QName name);

        /**
         * Overrides the logic used to determine the local name of a given attribute.
         * Defaults to {@link AttributeParser#getXmlName(AttributeDefinition)} if unspecified.
         * @param localNames a function used to determine the local name of a given attribute.
         * @return a reference to this builder.
         */
        default Builder withLocalNames(Function<AttributeDefinition, String> localNames) {
            return this.withNames(localNames.andThen(this::resolveQName));
        }

        /**
         * Provides a local name overrides for a specific set of attributes.
         * @param localNames a mapping of attribute to local name
         * @return a reference to this builder.
         */
        Builder withLocalNames(Map<AttributeDefinition, String> localNames);

        /**
         * Overrides the logic used to determine the local name of a given attribute.
         * Defaults to {@link AttributeParser#getXmlName(AttributeDefinition)} if unspecified.
         * @param localNames a function used to determine the local name of a given attribute.
         * @return a reference to this builder.
         */
        Builder withNames(Function<AttributeDefinition, QName> names);

        /**
         * Provides a local name overrides for a specific set of attributes.
         * @param localNames a mapping of attribute to local name
         * @return a reference to this builder.
         */
        Builder withNames(Map<AttributeDefinition, QName> names);

        /**
         * Overrides the logic used to determine the parser of a given attribute.
         * Defaults to {@link AttributeDefinition#getParser()} if unspecified.
         * @param parsers a function used to determine the parser of a given attribute.
         * @return a reference to this builder.
         */
        default Builder withParsers(Map<AttributeDefinition, AttributeParser> parsers) {
            return this.withParsers(new Function<>() {
                @Override
                public AttributeParser apply(AttributeDefinition attribute) {
                    return parsers.getOrDefault(attribute, attribute.getParser());
                }
            });
        }

        /**
         * Overrides the logic used to determine the parser of a given attribute.
         * Defaults to {@link AttributeDefinition#getAttributeParser()} if unspecified.
         * @param parsers a function used to determine the parser of a given attribute.
         * @return a reference to this builder.
         */
        Builder withParsers(Function<AttributeDefinition, AttributeParser> parsers);

        /**
         * Overrides the logic used to determine the parser of a given attribute.
         * Defaults to {@link AttributeDefinition#getParser()} if unspecified.
         * @param parsers a function used to determine the parser of a given attribute.
         * @return a reference to this builder.
         */
        default Builder withMarshallers(Map<AttributeDefinition, AttributeMarshaller> marshallers) {
            return this.withMarshallers(new Function<>() {
                @Override
                public AttributeMarshaller apply(AttributeDefinition attribute) {
                    return marshallers.getOrDefault(attribute, attribute.getMarshaller());
                }
            });
        }

        /**
         * Overrides the logic used to determine the parser of a given attribute.
         * Defaults to {@link AttributeDefinition#getAttributeParser()} if unspecified.
         * @param parsers a function used to determine the parser of a given attribute.
         * @return a reference to this builder.
         */
        Builder withMarshallers(Function<AttributeDefinition, AttributeMarshaller> marshallers);

        /**
         * Overrides the element local name of this resource.
         * @param localName the local element name override.
         * @return a reference to this builder.
         */
        default Builder withElementLocalName(String localName) {
            return this.withElementName(this.resolveQName(localName));
        }

        /**
         * Overrides the logic used to determine the element local name of this resource.
         * @see {@link ResourceXMLElementLocalName}
         * @param localName a function returning the element local name for a given path.
         * @return a reference to this builder.
         */
        default Builder withElementLocalName(Function<PathElement, String> localName) {
            return this.withElementName(localName.andThen(this::resolveQName));
        }

        /**
         * Overrides the logic used to determine the local element name of this resource.
         * @see {@link ResourceXMLElementLocalName}
         * @param function a function returning the qualified element name for a given path.
         * @return a reference to this builder.
         */
        default Builder withElementName(QName name) {
            return this.withElementName(new Function<>() {
                @Override
                public QName apply(PathElement path) {
                    return name;
                }
            });
        }

        /**
         * Overrides the logic used to determine the element local name of this resource.
         * @see {@link ResourceXMLElementLocalName}
         * @param name a function returning the qualified element name for a given path.
         * @return a reference to this builder.
         */
        Builder withElementName(Function<PathElement, QName> name);

        /**
         * Overrides the default element local names using the specific mapping.
         * If the attribute group does not exist in the specified map, those attributes will not be grouped within an element.
         * @param elementLocalNames a map of attribute group name to element local name
         * @return a reference to this builder.
         */
        default Builder withAttributeGroupElementLocalNames(Map<String, String> localNames) {
            return this.withAttributeGroupElementLocalNames(localNames::get);
        }

        /**
         * Overrides the logic used to determine the element local name of an attribute group.
         * Defaults to {@link UnaryOperator#identity()} if unspecified.
         * If the specified function returns null, attributes of a given a group will not be parsed/marshalled within an element.
         * @param elementLocalName a function returning the element local name of a given attribute group
         * @return a reference to this builder.
         */
        default Builder withAttributeGroupElementLocalNames(UnaryOperator<String> localNames) {
            return this.withAttributeGroupElementNames(new Function<>() {
                @Override
                public QName apply(String groupName) {
                    String localName = localNames.apply(groupName);
                    return (localName != null) ? Builder.this.resolveQName(localName) : null;
                }
            });
        }

        /**
         * Overrides the default element local names using the specific mapping.
         * If the attribute group does not exist in the specified map, those attributes will not be grouped within an element.
         * @param elementLocalNames a map of attribute group name to element local name
         * @return a reference to this builder.
         */
        default Builder withAttributeGroupElementNames(Map<String, QName> names) {
            return this.withAttributeGroupElementNames(names::get);
        }

        /**
         * Overrides the logic used to determine the element local name of an attribute group.
         * Defaults to {@link UnaryOperator#identity()} if unspecified.
         * If the specified function returns null, attributes of a given a group will not be parsed/marshalled within an element.
         * @param elementLocalName a function returning the element local name of a given attribute group
         * @return a reference to this builder.
         */
        Builder withAttributeGroupElementNames(Function<String, QName> names);

        /**
         * Applies the specified operation to the attributes of this resource.
         * Defaults to {@link UnaryOperator#identity()} if unspecified.
         * Used to filter and/or prepend/append attributes.
         * @param filter an attribute filter
         * @return a reference to this builder.
         */
        Builder filterAttributes(UnaryOperator<Stream<AttributeDefinition>> filter);

        /**
         * Includes the specified attributes.
         * @param includedAttributes a collection of included attributes
         * @return a reference to this builder.
         */
        default Builder includeAttribute(AttributeDefinition includedAttribute) {
            return this.filterAttributes(new UnaryOperator<>() {
                @Override
                public Stream<AttributeDefinition> apply(Stream<AttributeDefinition> attributes) {
                    return Stream.concat(attributes, Stream.of(includedAttribute));
                }
            });
        }

        /**
         * Includes the specified attributes.
         * @param includedAttributes a collection of included attributes
         * @return a reference to this builder.
         */
        default Builder includeAttributes(Collection<AttributeDefinition> includedAttributes) {
            return this.filterAttributes(new UnaryOperator<>() {
                @Override
                public Stream<AttributeDefinition> apply(Stream<AttributeDefinition> attributes) {
                    return Stream.concat(attributes, includedAttributes.stream());
                }
            });
        }

        /**
         * Includes an ignored attribute with the specified name.
         * @param attributeLocalName the local name of an attribute that should be ignored by the parser.
         * @return a reference to this builder.
         */
        default Builder ignoreAttribute(String attributeLocalName) {
            return this.includeAttribute(DefaultBuilder.createIgnoredAttributeDefinition(attributeLocalName));
        }

        /**
         * Includes a set of ignored attributes with the specified names in this description.
         * @param attributeLocalNames a set of attribute local names that should be ignored by the parser.
         * @return a reference to this builder.
         */
        default Builder ignoreAttributes(Set<String> attributeLocalNames) {
            return this.includeAttributes(attributeLocalNames.stream().map(DefaultBuilder::createIgnoredAttributeDefinition).toList());
        }

        /**
         * Excludes the specified attributes.
         * @param excludedAttributes a collection of excluded attributes
         * @return a reference to this builder.
         */
        default Builder excludeAttribute(AttributeDefinition excludedAttribute) {
            return this.filterAttributes(new UnaryOperator<>() {
                @Override
                public Stream<AttributeDefinition> apply(Stream<AttributeDefinition> attributes) {
                    return attributes.filter(Predicate.not(excludedAttribute::equals));
                }
            });
        }

        /**
         * Excludes the specified attributes.
         * @param excludedAttributes a collection of excluded attributes
         * @return a reference to this builder.
         */
        default Builder excludeAttributes(Collection<AttributeDefinition> excludedAttributes) {
            return this.filterAttributes(new UnaryOperator<>() {
                @Override
                public Stream<AttributeDefinition> apply(Stream<AttributeDefinition> attributes) {
                    return attributes.filter(Predicate.not(excludedAttributes::contains));
                }
            });
        }

        /**
         * Indicates that this element can be omitted if all of its attributes are undefined and any child resources are also empty.
         * @return a reference to this builder.
         */
        Builder omitIfEmpty();

        /**
         * Adds a child resource to this description to be written prior to writing any element based attributes of this resource.
         * @param description an XML description of a child resource.
         * @return a reference to this builder.
         */
        Builder insertChild(XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice);

        /**
         * Adds a child resource to this description to be written after writing all element based attributes of this resource.
         * @param description an XML description of a child resource.
         * @return a reference to this builder.
         */
        Builder appendChild(XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice);

        /**
         * Adds a child resource to this description to be written prior to writing any element based attributes of the specified attribute group.
         * @param attribute a grouped attribute.
         * @param choice a choice.
         * @return a reference to this builder.
         */
        Builder insertChild(AttributeDefinition attribute, XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice);

        /**
         * Adds a child resource to this description to be written after writing all element based attributes of the specified attribute group.
         * @param attribute a grouped attribute.
         * @param choice a choice.
         * @return a reference to this builder.
         */
        Builder appendChild(AttributeDefinition attribute, XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice);

        /**
         * Indicates that the operation associated with this resource should be discarded.
         * @return a reference to this builder.
         */
        default Builder thenDiscardOperation() {
            BiConsumer<Map<PathAddress, ModelNode>, PathAddress> remove = Map::remove;
            return this.withOperationTransformation(remove);
        }

        /**
         * Specifies an operation transformation function, applied after this resource and any children are parsed into an {@value ModelDescriptionConstants#ADD} operation.
         * Defaults to {@link UnaryOperator#identity()} if unspecified.
         * If this operator returns null, the {@value ModelDescriptionConstants#ADD} operation will be discarded.
         * @param transformer an operation transformer
         * @return a reference to this builder.
         */
        default Builder withOperationTransformation(UnaryOperator<ModelNode> transformer) {
            return this.withOperationTransformation(new BiFunction<>() {
                @Override
                public ModelNode apply(PathAddress key, ModelNode operation) {
                    return transformer.apply(operation);
                }
            });
        }

        /**
         * Specifies an operation remapping function, applied after this resource and any children are parsed into an {@value ModelDescriptionConstants#ADD} operation.
         * If this function returns null, the {@value ModelDescriptionConstants#ADD} operation will be discarded.
         * @param remappingFunction a remapping function for the current operation
         * @return a reference to this builder.
         */
        default Builder withOperationTransformation(BiFunction<PathAddress, ModelNode, ModelNode> remappingFunction) {
            return this.withOperationTransformation(new BiConsumer<>() {
                @Override
                public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                    operations.compute(operationKey, remappingFunction);
                }
            });
        }

        /**
         * Specifies an operation transformation function, applied after this resource and any children are parsed into an {@value ModelDescriptionConstants#ADD} operation.
         * Defaults to {@link Functions#discardingBiConsumer()} if unspecified.
         * @param transformation a consumer accepting all operations and the key of the current operation
         * @return a reference to this builder.
         */
        Builder withOperationTransformation(BiConsumer<Map<PathAddress, ModelNode>, PathAddress> transformation);

        /**
         * Builds an XML choice for this resource, using the specified override elements
         * @return an XML choice for this resource.
         */
        ResourceXMLChoice build(Collection<ResourceXMLElement> overrideElements);

        /**
         * Builds an XML element for this resource
         * @return an XML element for this resource.
         */
        ResourceXMLElement build();
    }

    static class DefaultBuilder implements Builder {
        private static final AttributeParser NO_OP_PARSER = new AttributeParser() {
            @Override
            public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
                // Do nothing
            }
        };
        private static final AttributeMarshaller NO_OP_MARSHALLER = new AttributeMarshaller() {
            @Override
            public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                // Do nothing
            }
        };
        static AttributeDefinition createIgnoredAttributeDefinition(String localName) {
            return new SimpleAttributeDefinitionBuilder(localName, ModelType.STRING)
                    .setRequired(false)
                    .setAttributeParser(NO_OP_PARSER)
                    .setAttributeMarshaller(NO_OP_MARSHALLER)
                    .build();
        }

        final FeatureFilter filter;
        final ResourceDescription description;
        final QNameResolver resolver;
        volatile Function<PathElement, QName> elementName;
        volatile QName pathValueAttributeName;
        volatile Function<Stream<AttributeDefinition>, Stream<AttributeDefinition>> attributesFilter = UnaryOperator.identity();
        volatile Function<AttributeDefinition, QName> names;
        volatile Function<AttributeDefinition, AttributeParser> parsers = AttributeDefinition::getParser;
        volatile Function<AttributeDefinition, AttributeMarshaller> marshallers = AttributeDefinition::getMarshaller;
        volatile Function<String, QName> groupElementNames;
        volatile Predicate<ModelNode> write = ModelNode::isDefined;
        volatile boolean omitIfEmpty = false;
        volatile BiConsumer<Map<PathAddress, ModelNode>, PathAddress> operationTransformation = Functions.discardingBiConsumer();
        volatile List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> preAttributeElements = List.of();
        volatile List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> postAttributeElements = List.of();
        volatile Map<String, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> preAttributeGroupElements = Map.of();
        volatile Map<String, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> postAttributeGroupElements = Map.of();
        volatile boolean required = false;

        protected DefaultBuilder(FeatureFilter filter, ResourceDescription description, QNameResolver qualifiedNameFactory) {
            this.filter = filter;
            this.description = description;
            this.resolver = qualifiedNameFactory;
            boolean wildcard = description.getPathElement().isWildcard();
            this.pathValueAttributeName = wildcard ? this.resolver.resolveQName(ModelDescriptionConstants.NAME) : null;
            Function<String, QName> elementName = this.resolver::resolveQName;
            this.elementName = elementName.compose(wildcard ? ResourceXMLElementLocalName.KEY : ResourceXMLElementLocalName.VALUE);
            this.groupElementNames = elementName;
            this.names = elementName.compose(new Function<>() {
                @Override
                public String apply(AttributeDefinition attribute) {
                    return DefaultBuilder.this.getParser(attribute).getXmlName(attribute);
                }
            });
        }

        private AttributeParser getParser(AttributeDefinition attribute) {
            return this.parsers.apply(attribute);
        }

        @Override
        public QName resolveQName(String localName) {
            return this.resolver.resolveQName(localName);
        }

        @Override
        public Builder require() {
            this.required = true;
            return this;
        }

        @Override
        public Builder withPathValueAttributeName(QName name) {
            this.pathValueAttributeName = name;
            return this;
        }

        @Override
        public Builder withLocalNames(Map<AttributeDefinition, String> localNames) {
            Function<AttributeDefinition, QName> defaultNames = this.names;
            return this.withLocalNames(new Function<>() {
                @Override
                public String apply(AttributeDefinition attribute) {
                    return localNames.getOrDefault(attribute, defaultNames.apply(attribute).getLocalPart());
                }
            });
        }

        @Override
        public Builder withNames(Function<AttributeDefinition, QName> names) {
            this.names = names;
            return this;
        }

        @Override
        public Builder withNames(Map<AttributeDefinition, QName> names) {
            Function<AttributeDefinition, QName> defaultNames = this.names;
            return this.withNames(new Function<>() {
                @Override
                public QName apply(AttributeDefinition attribute) {
                    return names.getOrDefault(attribute, defaultNames.apply(attribute));
                }
            });
        }

        @Override
        public Builder withParsers(Function<AttributeDefinition, AttributeParser> parsers) {
            this.parsers = parsers;
            return this;
        }

        @Override
        public Builder withMarshallers(Function<AttributeDefinition, AttributeMarshaller> marshallers) {
            this.marshallers = marshallers;
            return this;
        }

        @Override
        public Builder withElementName(Function<PathElement, QName> elementName) {
            this.elementName = elementName;
            return this;
        }

        @Override
        public Builder withAttributeGroupElementNames(Function<String, QName> groupElementNames) {
            this.groupElementNames = groupElementNames;
            return this;
        }

        @Override
        public Builder filterAttributes(UnaryOperator<Stream<AttributeDefinition>> filter) {
            this.attributesFilter = this.attributesFilter.andThen(filter);
            return this;
        }

        @Override
        public Builder insertChild(AttributeDefinition attribute, XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice) {
            String group = attribute.getAttributeGroup();
            Assert.assertNotNull(group);
            if (this.preAttributeGroupElements.isEmpty()) {
                this.preAttributeGroupElements = Map.of(group, new LinkedList<>());
            }
            List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> choices = this.preAttributeGroupElements.get(group);
            if (choices == null) {
                choices = new LinkedList<>();
                if (this.preAttributeGroupElements.size() == 1) {
                    this.preAttributeGroupElements = new HashMap<>(this.preAttributeGroupElements);
                }
                this.preAttributeGroupElements.put(group, choices);
            }
            choices.add(choice);
            return this;
        }

        @Override
        public Builder appendChild(AttributeDefinition attribute, XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice) {
            String group = attribute.getAttributeGroup();
            Assert.assertNotNull(group);
            if (this.postAttributeGroupElements.isEmpty()) {
                this.postAttributeGroupElements = Map.of(group, new LinkedList<>());
            }
            List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> choices = this.postAttributeGroupElements.get(group);
            if (choices == null) {
                choices = new LinkedList<>();
                if (this.postAttributeGroupElements.size() == 1) {
                    this.postAttributeGroupElements = new HashMap<>(this.postAttributeGroupElements);
                }
                this.postAttributeGroupElements.put(group, choices);
            }
            choices.add(choice);
            return this;
        }

        @Override
        public Builder insertChild(XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice) {
            if (this.preAttributeElements.isEmpty()) {
                this.preAttributeElements = new LinkedList<>();
            }
            this.preAttributeElements.add(choice);
            return this;
        }

        @Override
        public Builder appendChild(XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice) {
            if (this.postAttributeElements.isEmpty()) {
                this.postAttributeElements = new LinkedList<>();
            }
            this.postAttributeElements.add(choice);
            return this;
        }

        @Override
        public Builder omitIfEmpty() {
            this.omitIfEmpty = true;
            return this;
        }

        @Override
        public Builder withOperationTransformation(BiConsumer<Map<PathAddress, ModelNode>, PathAddress> transformation) {
            this.operationTransformation = this.operationTransformation.andThen(transformation);
            return this;
        }

        @Override
        public ResourceXMLElement build() {
            ResourceDescription description = this.description;
            PathElement path = description.getPathElement();
            QName name = this.elementName.apply(path);

            Function<Stream<AttributeDefinition>, Stream<AttributeDefinition>> attributesFilter = this.attributesFilter;
            FeatureFilter filter = this.filter;
            Supplier<Stream<AttributeDefinition>> attributesProvider = new Supplier<>() {
                @Override
                public Stream<AttributeDefinition> get() {
                    return attributesFilter.apply(description.getAttributes()).filter(filter::enables);
                }
            };
            QName pathValueAttributeName = this.pathValueAttributeName;
            // Pseudo attribute for the path value
            AttributeDefinition pathValueAttribute = (pathValueAttributeName != null) ? createIgnoredAttributeDefinition(UUID.randomUUID().toString()) : null;

            Function<AttributeDefinition, QName> configuredNames = this.names;
            Function<AttributeDefinition, QName> names = (pathValueAttribute != null) ? new Function<>() {
                @Override
                public QName apply(AttributeDefinition attribute) {
                    // Associate path value attribute with its QName
                    return (attribute == pathValueAttribute) ? pathValueAttributeName : configuredNames.apply(attribute);
                }
            } : configuredNames;

            Function<AttributeDefinition, AttributeParser> parsers = this.parsers;
            Function<AttributeDefinition, AttributeMarshaller> marshallers = this.marshallers;
            Function<String, QName> groupElementNames = this.groupElementNames;
            List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> preAttributeElements = List.copyOf(this.preAttributeElements);
            List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> postAttributeElements = List.copyOf(this.postAttributeElements);
            boolean omitIfEmpty = this.omitIfEmpty;

            BiConsumer<Map<PathAddress, ModelNode>, PathAddress> operationTransformation = this.operationTransformation;
            Map<QName, Map.Entry<Map<QName, AttributeDefinition>, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>>> groups = this.preAttributeGroupElements.isEmpty() && this.postAttributeGroupElements.isEmpty() ? Map.of() : new LinkedHashMap<>();

            // Pre-attribute group elements
            for (Map.Entry<String, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> groupElements : this.preAttributeGroupElements.entrySet()) {
                QName groupName = this.groupElementNames.apply(groupElements.getKey());
                Map.Entry<Map<QName, AttributeDefinition>, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> group = groups.get(groupName);
                if (group == null) {
                    group = new AbstractMap.SimpleEntry<>(new HashMap<>(), new LinkedList<>());
                    groups.put(groupName, group);
                }
                for (XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice : groupElements.getValue()) {
                    if (this.filter.enables(choice)) {
                        group.getValue().add(choice);
                    }
                }
            }

            Iterator<AttributeDefinition> resourceAttributes = attributesProvider.get().iterator();
            Map.Entry<Map<QName, AttributeDefinition>, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> defaultGroup = new AbstractMap.SimpleEntry<>(resourceAttributes.hasNext() || (pathValueAttribute != null) ? new HashMap<>() : Map.of(), preAttributeElements.isEmpty() && postAttributeElements.isEmpty() ? List.of() : new LinkedList<>());

            if (pathValueAttribute != null) {
                defaultGroup.getKey().put(pathValueAttributeName, pathValueAttribute);
            }

            // Early children
            for (XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice : preAttributeElements) {
                if (this.filter.enables(choice)) {
                    defaultGroup.getValue().add(choice);
                }
            }

            // Process attributes and groups
            while (resourceAttributes.hasNext()) {
                AttributeDefinition attribute = resourceAttributes.next();
                QName attributeGroupName = Optional.ofNullable(attribute.getAttributeGroup()).map(groupElementNames).orElse(null);
                Map.Entry<Map<QName, AttributeDefinition>, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> group = (attributeGroupName == null) ? defaultGroup : groups.get(attributeGroupName);
                if (group == null) {
                    if (groups.isEmpty()) {
                        groups = new LinkedHashMap<>();
                    }
                    group = new AbstractMap.SimpleEntry<>(new HashMap<>(), List.of());
                    groups.put(attributeGroupName, group);
                }
                AttributeParser parser = parsers.apply(attribute);
                AttributeMarshaller marshaller = marshallers.apply(attribute);
                QName attributeName = names.apply(attribute);
                if (parser.isParseAsElement() || marshaller.isMarshallableAsElement()) {
                    if (group.getValue().isEmpty()) {
                        group.setValue(new LinkedList<>());
                    }
                    group.getValue().add(new ResourceAttributeXMLElement(attributeName, attribute, parser, marshaller));
                } else if (!parser.isParseAsElement() || !marshaller.isMarshallableAsElement()) {
                    AttributeDefinition existing = group.getKey().put(attributeName, attribute);
                    if (existing != null) {
                        throw ClusteringLogger.ROOT_LOGGER.duplicateAttributes(Optional.ofNullable(attributeGroupName).orElse(name), attributeName);
                    }
                }
            }

            // Post-attribute group elements
            for (Map.Entry<String, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> groupElements : this.postAttributeGroupElements.entrySet()) {
                QName groupName = this.groupElementNames.apply(groupElements.getKey());
                Map.Entry<Map<QName, AttributeDefinition>, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>> group = groups.get(groupName);
                if (group.getValue().isEmpty()) {
                    group.setValue(new LinkedList<>());
                }
                for (XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice : groupElements.getValue()) {
                    if (this.filter.enables(choice)) {
                        group.getValue().add(choice);
                    }
                }
            }

            // Attribute group elements
            if (!groups.isEmpty()) {
                if (defaultGroup.getValue().isEmpty()) {
                    defaultGroup.setValue(new LinkedList<>());
                }
                for (Map.Entry<QName, Map.Entry<Map<QName, AttributeDefinition>, List<XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>>>> groupEntry : groups.entrySet()) {
                    Map<QName, AttributeDefinition> groupAttributes = groupEntry.getValue().getKey();
                    XMLContentReader<ModelNode> attributesReader = new ResourceAttributesXMLContentReader(groupAttributes, parsers);
                    XMLContentWriter<ModelNode> attributesWriter = new ResourceAttributesXMLContentWriter(groupAttributes.values(), marshallers);
                    XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> childContent = XMLContent.all(groupEntry.getValue().getValue());
                    defaultGroup.getValue().add(new ResourceOperationXMLElement<>(groupEntry.getKey(), attributesReader, attributesWriter, Function.identity(), childContent));
                }
            }

            // Late children
            for (XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> choice : postAttributeElements) {
                if (this.filter.enables(choice)) {
                    defaultGroup.getValue().add(choice);
                }
            }

            XMLCardinality cardinality = path.isWildcard() ? (this.required ? XMLCardinality.Unbounded.REQUIRED : XMLCardinality.Unbounded.OPTIONAL) : (this.required ? XMLCardinality.Single.REQUIRED : XMLCardinality.Single.OPTIONAL);

            Map<QName, AttributeDefinition> defaultGroupAttributes = defaultGroup.getKey();
            XMLContentReader<ModelNode> attributesReader = new ResourceAttributesXMLContentReader(defaultGroupAttributes, parsers);
            XMLContentWriter<ModelNode> attributesWriter = new ResourceAttributesXMLContentWriter(defaultGroupAttributes.values(), marshallers);
            XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> childContent = XMLContent.all(defaultGroup.getValue());
            ResourceOperationXMLElement<Property> element = new ResourceOperationXMLElement<>(name, attributesReader, new ResourceEntryAttributesXMLContentWriter(pathValueAttributeName, attributesWriter), Property::getValue, childContent);

            return new AbstractResourceXMLElement() {
                @Override
                public QName getName() {
                    return name;
                }

                @Override
                public PathElement getPathElement() {
                    return path;
                }

                @Override
                public Stability getStability() {
                    return description.getStability();
                }

                @Override
                public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                    PathAddress parentOperationKey = context.getKey();
                    Map<PathAddress, ModelNode> operations = context.getValue();

                    String value = (pathValueAttributeName != null) ? reader.getAttributeValue(null, pathValueAttributeName.getLocalPart()) : null;
                    if (path.isWildcard() && (value == null)) {
                        throw ParseUtils.missingRequired(reader, pathValueAttributeName.getLocalPart());
                    }

                    ModelNode parentOperation = (parentOperationKey.size() > 0) ? operations.get(parentOperationKey) : null;
                    PathAddress parentAddress = (parentOperation != null) ? PathAddress.pathAddress(parentOperation.get(ModelDescriptionConstants.OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
                    PathAddress operationAddress = parentAddress.append(path.isWildcard() ? PathElement.pathElement(path.getKey(), value) : path);
                    PathAddress operationKey = path.isWildcard() ? operationAddress : parentOperationKey.append(description.getPathKey());
                    ModelNode operation = Util.createAddOperation(operationAddress);
                    operations.put(operationKey, operation);

                    element.readElement(reader, Map.entry(operationKey, operations));
                    operationTransformation.accept(operations, operationKey);
                }

                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode parentModel) throws XMLStreamException {
                    String key = path.getKey();
                    if (parentModel.hasDefined(key)) {
                        ModelNode keyModel = parentModel.get(key);

                        List<Property> properties = path.isWildcard() ? keyModel.asPropertyList() : (keyModel.hasDefined(path.getValue()) ? List.of(new Property(path.getValue(), keyModel.get(path.getValue()))) : List.<Property>of());
                        for (Property property : properties) {
                            if (!omitIfEmpty || !element.isEmpty(property)) {
                                element.writeContent(writer, property);
                            }
                        }
                    }
                }

                @Override
                public boolean isEmpty(ModelNode parentModel) {
                    return !parentModel.hasDefined(path.getKey()) || (!path.isWildcard() && !parentModel.hasDefined(path.getKeyValuePair()));
                }

                @Override
                public XMLCardinality getCardinality() {
                    return cardinality;
                }
            };
        }

        @Override
        public ResourceXMLChoice build(Collection<ResourceXMLElement> overrideElements) {
            // Build element for wildcard registration
            ResourceXMLElement element = this.build();
            if (overrideElements.isEmpty()) return element;

            ResourceDescription description = this.description;
            PathElement elementPath = description.getPathElement();
            Assert.assertTrue(elementPath.isWildcard());

            Map<QName, Map<PathElement, XMLContentReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>>>> readers = new HashMap<>();
            Map<PathElement, ResourceXMLElement> writers = new HashMap<>();
            readers.put(element.getName(), Map.of(element.getPathElement(), element));
            writers.put(element.getPathElement(), element);
            for (ResourceXMLElement overrideElement : overrideElements) {
                QName overrideName = overrideElement.getName();
                Map<PathElement, XMLContentReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>>> paths = readers.get(overrideName);
                if (paths == null) {
                    paths = new HashMap<>();
                    readers.put(overrideName, paths);
                }
                PathElement overridePath = overrideElement.getPathElement();
                Assert.assertFalse(overridePath.isWildcard());
                Assert.assertTrue(elementPath.getKey().equals(overridePath.getKey()));
                paths.put(overridePath, overrideElement);
                writers.put(overridePath, overrideElement);
            }
            QName pathValueAttributeName = this.pathValueAttributeName;

            return new AbstractResourceXMLChoice() {
                @Override
                public Set<QName> getChoices() {
                    return readers.keySet();
                }

                @Override
                public Set<PathElement> getPathElements() {
                    return writers.keySet();
                }

                @Override
                public XMLContentReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> getReader(QName name) {
                    Map<PathElement, XMLContentReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>>> elements = readers.get(name);
                    return new XMLContentReader<>() {
                        @Override
                        public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                            String value = reader.getAttributeValue(null, pathValueAttributeName.getLocalPart());
                            if (value == null) {
                                throw ParseUtils.missingRequired(reader, pathValueAttributeName.getLocalPart());
                            }
                            PathElement path = PathElement.pathElement(elementPath.getKey(), value);
                            elements.getOrDefault(path, element).readElement(reader, context);
                        }

                        @Override
                        public XMLCardinality getCardinality() {
                            return XMLCardinality.Unbounded.OPTIONAL;
                        }
                    };
                }

                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode parent) throws XMLStreamException {
                    PathElement path = element.getPathElement();
                    String key = path.getKey();
                    if (parent.hasDefined(key)) {
                        ModelNode keyModel = parent.get(key);

                        List<Property> properties = path.isWildcard() ? keyModel.asPropertyList() : (keyModel.hasDefined(path.getValue()) ? List.of(new Property(path.getValue(), keyModel.get(path.getValue()))) : List.<Property>of());
                        for (Property property : properties) {
                            String value = property.getName();
                            ModelNode model = property.getValue();

                            PathElement overridePath = PathElement.pathElement(key, value);
                            ModelNode parentWrapper = new ModelNode();
                            parentWrapper.get(overridePath.getKeyValuePair()).set(model);
                            writers.getOrDefault(overridePath, element).writeContent(writer, parentWrapper);
                        }
                    }
                }

                @Override
                public Stability getStability() {
                    Stability stability = element.getStability();
                    for (ResourceXMLElement overrideElement : overrideElements) {
                        if (stability.enables(overrideElement.getStability())) {
                            stability = overrideElement.getStability();
                        }
                    }
                    return stability;
                }

                @Override
                public boolean isEmpty(ModelNode parent) {
                    return !parent.hasDefined(element.getPathElement().getKey());
                }

                @Override
                public XMLCardinality getCardinality() {
                    return element.getCardinality();
                }
            };
        }
    }

    abstract class AbstractResourceXMLElement extends AbstractXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLElement {
    }

    abstract class AbstractResourceXMLChoice extends AbstractXMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLChoice {
    }
}

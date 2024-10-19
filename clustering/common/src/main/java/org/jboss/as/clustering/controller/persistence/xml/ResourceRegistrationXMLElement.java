/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.function.Functions;

/**
 * Encapsulates an XML element for a resource registration.
 * @author Paul Ferraro
 */
public interface ResourceRegistrationXMLElement extends ResourceXMLElement, ResourceRegistration {

    interface Builder<T extends ResourceRegistrationXMLElement, B extends Builder<T, B>> extends ResourceXMLContainer.Builder<T, B> {
        /**
         * Overrides the key used to index the generated operation.
         * @param operationKey an operation key
         * @return a reference to this builder.
         */
        B withOperationKey(PathElement operationKey);

        /**
         * Indicates that this resource is required to be present.
         * This is a convenience method that delegates to {@link #withCardinality(XMLCardinality)}.
         * @return a reference to this builder.
         */
        B require();

        /**
         * Overrides the local name of the XML element for this resource.
         * @param localName the local element name override.
         * @return a reference to this builder.
         */
        B withElementLocalName(String localName);

        /**
         * Overrides the logic used to determine the local name of the XML element for this resource.
         * @see {@link ResourceXMLElementLocalName}
         * @param localName a function returning the element local name for a given path.
         * @return a reference to this builder.
         */
        B withElementLocalName(Function<PathElement, String> localName);

        /**
         * Overrides the logic used to determine the local element name of this resource.
         * @see {@link ResourceXMLElementLocalName}
         * @param function a function returning the qualified element name for a given path.
         * @return a reference to this builder.
         */
        default B withElementName(QName name) {
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
        B withElementName(Function<PathElement, QName> name);

        /**
         * Indicates that the operation associated with this resource should be discarded.
         * @return a reference to this builder.
         */
        default B thenDiscardOperation() {
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
        default B withOperationTransformation(UnaryOperator<ModelNode> transformer) {
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
        default B withOperationTransformation(BiFunction<PathAddress, ModelNode, ModelNode> remappingFunction) {
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
        B withOperationTransformation(BiConsumer<Map<PathAddress, ModelNode>, PathAddress> transformation);
    }

    abstract class AbstractBuilder<T extends ResourceRegistrationXMLElement, B extends Builder<T, B>> extends ResourceXMLContainer.AbstractBuilder<T, B> implements Builder<T, B> {
        private final ResourceRegistration registration;
        private volatile BiConsumer<Map<PathAddress, ModelNode>, PathAddress> operationTransformation = Functions.discardingBiConsumer();
        private volatile Function<PathElement, QName> elementName;
        private volatile Optional<PathElement> operationKey = Optional.empty();

        protected AbstractBuilder(ResourceRegistration registration, FeatureFilter filter, QNameResolver resolver) {
            super(filter, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
            this.registration = registration;
            Function<PathElement, String> localName = this.registration.getPathElement().isWildcard() ? ResourceXMLElementLocalName.KEY : ResourceXMLElementLocalName.VALUE;
            this.elementName = localName.andThen(resolver::resolve);
        }

        @Override
        public B withOperationKey(PathElement operationKey) {
            this.operationKey = Optional.of(operationKey);
            return this.builder();
        }

        @Override
        public B require() {
            return this.withCardinality(this.registration.getPathElement().isWildcard() ? XMLCardinality.Unbounded.REQUIRED : XMLCardinality.Single.REQUIRED);
        }

        @Override
        public B withElementLocalName(String localName) {
            return this.withElementName(this.resolve(localName));
        }

        @Override
        public B withElementLocalName(Function<PathElement, String> localName) {
            return this.withElementName(localName.andThen(this::resolve));
        }

        @Override
        public B withElementName(Function<PathElement, QName> elementName) {
            this.elementName = elementName;
            return this.builder();
        }

        @Override
        public B withOperationTransformation(BiConsumer<Map<PathAddress, ModelNode>, PathAddress> transformation) {
            this.operationTransformation = this.operationTransformation.andThen(transformation);
            return this.builder();
        }

        ResourceRegistration getResourceRegistration() {
            return this.registration;
        }

        BiConsumer<Map<PathAddress, ModelNode>, PathAddress> getOperationTransformation() {
            return this.operationTransformation;
        }

        Function<PathElement, QName> getElementName() {
            return this.elementName;
        }

        Optional<PathElement> getOperationKey() {
            return this.operationKey;
        }
    }

    class DefaultResourceRegistrationXMLElement extends DefaultXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceRegistrationXMLElement {
        private final ResourceRegistration registration;

        DefaultResourceRegistrationXMLElement(ResourceRegistration registration, QName name, XMLCardinality cardinality, XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader, XMLContentWriter<ModelNode> writer) {
            super(name, cardinality, reader, writer, registration.getStability());
            this.registration = registration;
        }

        @Override
        public PathElement getPathElement() {
            return this.registration.getPathElement();
        }
    }

    class ResourcePropertyAttributesXMLContentWriter implements XMLContentWriter<Property> {
        private final XMLContentWriter<ModelNode> attributesWriter;

        ResourcePropertyAttributesXMLContentWriter(Collection<AttributeDefinition> attributes, AttributeDefinitionXMLConfiguration configuration) {
            this.attributesWriter = new ResourceAttributesXMLContentWriter(attributes, configuration);
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
            this.attributesWriter.writeContent(writer, property.getValue());
        }

        @Override
        public boolean isEmpty(Property property) {
            return this.attributesWriter.isEmpty(property.getValue());
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLParticle;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;

/**
 * Encapsulates an XML choice for a wildcard resource registration and its overrides.
 * @author Paul Ferraro
 */
public interface ResourceRegistrationXMLChoice extends ResourceXMLChoice {

    interface Builder extends XMLParticle.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, ResourceRegistrationXMLChoice, Builder> {
        /**
         * Adds an override element to this choice.
         * @param element an override element
         * @return a reference to this builder
         */
        Builder addElement(NamedResourceRegistrationXMLElement element);
    }

    static ResourceRegistrationXMLChoice singleton(ResourceRegistrationXMLElement element) {
        return new DefaultResourceRegistrationXMLChoice(Set.of(element.getName()), element.getCardinality(), new XMLElementReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                // Validate entry criteria
                Assert.assertTrue(reader.isStartElement());
                int occurrences = 0;
                if (element.getName().equals(reader.getName())) {
                    int maxOccurs = element.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
                    do {
                        occurrences += 1;
                        if (occurrences > maxOccurs) {
                            throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, Set.of(element.getName()), element.getCardinality());
                        }
                        element.getReader().readElement(reader, context);
                    } while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT && reader.getName().equals(element.getName()));
                }
                if (occurrences < element.getCardinality().getMinOccurs()) {
                    throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, Set.of(element.getName()), element.getCardinality());
                }
            }
        }, element.getWriter(), element.getStability());
    }

    class DefaultBuilder extends XMLParticle.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, ResourceRegistrationXMLChoice, Builder> implements Builder {
        // Special case comparator that only compares path values
        static final Comparator<PathElement> PATH_COMPARATOR = Comparator.comparing(PathElement::getValue);
        private final NamedResourceRegistrationXMLElement element;
        private final Map<PathElement, ResourceRegistrationXMLElement> overrides = new TreeMap<>(PATH_COMPARATOR);
        private final FeatureFilter filter;

        DefaultBuilder(NamedResourceRegistrationXMLElement element, FeatureFilter filter) {
            Assert.assertTrue(element.getPathElement().isWildcard());
            this.element = element;
            this.filter = filter;
        }

        @Override
        protected Builder builder() {
            return this;
        }

        @Override
        public Builder addElement(NamedResourceRegistrationXMLElement element) {
            if (this.filter.enables(element)) {
                PathElement path = element.getPathElement();
                Assert.assertFalse(path.isWildcard());
                Assert.assertTrue(path.getKey().equals(this.element.getPathElement().getKey()));
                if (this.overrides.putIfAbsent(path, element) != null) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicatePathElement(path);
                }
            }
            return this;
        }

        @Override
        public ResourceRegistrationXMLChoice build() {
            if (this.overrides.isEmpty()) return singleton(this.element);

            PathElement wildcardPath = this.element.getPathElement();
            QName pathValueAttributeName = this.element.getResourceAttributeName();
            XMLCardinality cardinality = this.getCardinality();

            Function<PathElement, ResourceRegistrationXMLElement> elements = path -> this.overrides.getOrDefault(path, this.element);

            Map<QName, Map<PathElement, ResourceRegistrationXMLElement>> choices = new TreeMap<>(QNameResolver.COMPARATOR);
            Map<PathElement, ResourceRegistrationXMLElement> defaultChoice = new TreeMap<>(PATH_COMPARATOR);
            defaultChoice.put(wildcardPath, this.element);
            choices.put(this.element.getName(), defaultChoice);
            for (ResourceRegistrationXMLElement override : this.overrides.values()) {
                Map<PathElement, ResourceRegistrationXMLElement> choiceElements = choices.get(override.getName());
                if (choiceElements == null) {
                    choiceElements = new TreeMap<>(PATH_COMPARATOR);
                    choices.put(override.getName(), choiceElements);
                }
                choiceElements.put(override.getPathElement(), override);
            }

            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader = new XMLElementReader<>() {
                private ResourceRegistrationXMLElement findElement(XMLExtendedStreamReader reader) throws XMLStreamException {
                    Map<PathElement, ResourceRegistrationXMLElement> choice = choices.get(reader.getName());
                    if (choice != null) {
                        String value = reader.getAttributeValue(null, pathValueAttributeName.getLocalPart());
                        if (value == null) {
                            throw ParseUtils.missingRequired(reader, pathValueAttributeName.getLocalPart());
                        }
                        ResourceRegistrationXMLElement element = choice.get(PathElement.pathElement(wildcardPath.getKey(), value));
                        return (element != null) ? element : choice.get(wildcardPath);
                    }
                    return null;
                }

                @Override
                public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    int occurrences = 0;
                    ResourceRegistrationXMLElement element = this.findElement(reader);
                    if (element != null) {
                        int maxOccurs = element.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
                        do {
                            occurrences += 1;
                            // Validate maxOccurs
                            if (occurrences > maxOccurs) {
                                throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, Set.of(element.getName()), element.getCardinality());
                            }
                            element.getReader().readElement(reader, context);
                        } while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT) && (this.findElement(reader) == element));
                        // Validate minOccurs
                        if (occurrences < element.getCardinality().getMinOccurs()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, Set.of(element.getName()), element.getCardinality());
                        }
                    } else if (cardinality.isRequired()) {
                        throw ParseUtils.unexpectedElement(reader, choices.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }
                }
            };
            XMLContentWriter<ModelNode> writer = new XMLContentWriter<>() {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode parent) throws XMLStreamException {
                    String key = wildcardPath.getKey();
                    if (parent.hasDefined(key)) {
                        for (Property property : parent.get(key).asPropertyListOrEmpty()) {
                            String value = property.getName();
                            ModelNode model = property.getValue();

                            PathElement path = PathElement.pathElement(key, value);
                            ModelNode parentWrapper = new ModelNode();
                            parentWrapper.get(path.getKeyValuePair()).set(model);
                            elements.apply(path).getWriter().writeContent(writer, parentWrapper);
                        }
                    }
                }

                @Override
                public boolean isEmpty(ModelNode parent) {
                    return !parent.hasDefined(wildcardPath.getKey()) || parent.get(wildcardPath.getKey()).asPropertyListOrEmpty().isEmpty();
                }
            };
            return new DefaultResourceRegistrationXMLChoice(choices.keySet(), cardinality, reader, writer, this.element.getStability());
        }
    }

    class DefaultResourceRegistrationXMLChoice extends DefaultXMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceRegistrationXMLChoice {

        DefaultResourceRegistrationXMLChoice(Set<QName> names, XMLCardinality cardinality, XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader, XMLContentWriter<ModelNode> writer, Stability stability) {
            super(names, cardinality, reader, writer, stability);
        }
    }
}

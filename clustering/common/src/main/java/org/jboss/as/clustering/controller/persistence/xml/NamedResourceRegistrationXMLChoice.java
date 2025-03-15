/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.clustering.controller.xml.XMLElementGroup;
import org.jboss.as.clustering.controller.xml.XMLParticle;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.common.function.Functions;

/**
 * Encapsulates an XML choice for a wildcard resource registration and its overrides.
 * @author Paul Ferraro
 */
public interface NamedResourceRegistrationXMLChoice extends ResourceXMLChoice {

    interface Builder extends XMLElementGroup.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, NamedResourceRegistrationXMLElement, NamedResourceRegistrationXMLChoice, Builder> {
    }

    class DefaultBuilder extends XMLParticle.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, NamedResourceRegistrationXMLChoice, Builder> implements Builder {
        // Special case comparator that only compares path values
        static final Comparator<PathElement> PATH_COMPARATOR = Comparator.comparing(PathElement::getValue);
        private final NamedResourceRegistrationXMLElement element;
        private final Map<PathElement, ResourceRegistrationXMLElement> overrides = new TreeMap<>(PATH_COMPARATOR);
        private final FeatureFilter filter;

        DefaultBuilder(NamedResourceRegistrationXMLElement element, FeatureFilter filter) {
            Assert.assertTrue(element.getPathElement().isWildcard());
            this.element = element;
            this.filter = filter;
            this.withCardinality(element.getCardinality());
        }

        @Override
        protected Builder builder() {
            return this;
        }

        @Override
        public Builder addElement(NamedResourceRegistrationXMLElement element) {
            if (this.filter.enables(element) && element.getCardinality().isEnabled()) {
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
        public NamedResourceRegistrationXMLChoice build() {
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
            return new DefaultResourceRegistrationXMLChoice(this.element, pathValueAttributeName, choices, elements, cardinality);
        }
    }

    class DefaultResourceRegistrationXMLChoice extends DefaultXMLElementChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements NamedResourceRegistrationXMLChoice {

        DefaultResourceRegistrationXMLChoice(ResourceRegistration registration, QName pathValueAttributeName, Map<QName, Map<PathElement, ResourceRegistrationXMLElement>> choices, Function<PathElement, ResourceRegistrationXMLElement> elements, XMLCardinality cardinality) {
            super(choices.keySet(), cardinality, new ExceptionFunction<>() {
                @Override
                public XMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> apply(XMLExtendedStreamReader reader) throws XMLStreamException {
                    Map<PathElement, ResourceRegistrationXMLElement> choice = choices.get(reader.getName());
                    if (choice != null) {
                        String value = reader.getAttributeValue(null, pathValueAttributeName.getLocalPart());
                        if (value == null) {
                            throw ParseUtils.missingRequired(reader, pathValueAttributeName.getLocalPart());
                        }
                        // Look for override resource registration first
                        ResourceRegistrationXMLElement element = choice.get(PathElement.pathElement(registration.getPathElement().getKey(), value));
                        // Fallback to wildcard resource registration
                        return (element != null) ? element : choice.get(registration.getPathElement());
                    }
                    return null;
                }
            }, Functions.discardingConsumer(), new XMLContentWriter<>() {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode parent) throws XMLStreamException {
                    String key = registration.getPathElement().getKey();
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
                    String pathKey = registration.getPathElement().getKey();
                    return !parent.hasDefined(pathKey) || parent.get(pathKey).asPropertyListOrEmpty().isEmpty();
                }
            }, registration.getStability());
        }
    }
}

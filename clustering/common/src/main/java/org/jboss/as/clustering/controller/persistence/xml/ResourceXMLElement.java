/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Encapsulates an XML element for a subsystem resource.
 */
public interface ResourceXMLElement extends ResourceXMLContainer, XMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder extends ResourceXMLContainer.Builder<ResourceXMLElement, Builder> {
    }

    class DefaultBuilder extends ResourceXMLContainer.AbstractBuilder<ResourceXMLElement, Builder> implements Builder {
        private final QName name;
        private final Stability stability;

        DefaultBuilder(QName name, Stability stability, FeatureRegistry registry, QNameResolver resolver) {
            this(name, stability, registry, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
        }

        DefaultBuilder(QName name, Stability stability, FeatureRegistry registry, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(registry, resolver, configuration);
            this.name = name;
            this.stability = stability;
        }

        @Override
        protected ResourceXMLElement.Builder builder() {
            return this;
        }

        @Override
        public ResourceXMLElement build() {
            return new DefaultResourceContentXMLElement(this.name, this.getCardinality(), this.getAttributes(), this.getConfiguration(), this.getContent(), this.stability);
        }
    }

    class DefaultResourceContentXMLElement extends DefaultXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLElement {

        DefaultResourceContentXMLElement(QName name, XMLCardinality cardinality, Collection<AttributeDefinition> attributes, AttributeDefinitionXMLConfiguration configuration, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content, Stability stability) {
            this(name, cardinality, new ResourceAttributesXMLContentReader(attributes, configuration), new ResourceAttributesXMLContentWriter(attributes, configuration), content, stability);
        }

        private DefaultResourceContentXMLElement(QName name, XMLCardinality cardinality, XMLElementReader<ModelNode> attributesReader, XMLContentWriter<ModelNode> attributesWriter, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content, Stability stability) {
            super(name, cardinality, new ResourceXMLContainerReader(attributesReader, content), new XMLContentWriter<>() {
                private final XMLContentWriter<ModelNode> containerWriter = new ResourceXMLContainerWriter<>(name, attributesWriter, Function.identity(), content);

                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode content) throws XMLStreamException {
                    // Omit if empty
                    if (!this.containerWriter.isEmpty(content)) {
                        this.containerWriter.writeContent(writer, content);
                    }
                }

                @Override
                public boolean isEmpty(ModelNode content) {
                    return this.containerWriter.isEmpty(content);
                }
            }, stability);
        }
    }
}

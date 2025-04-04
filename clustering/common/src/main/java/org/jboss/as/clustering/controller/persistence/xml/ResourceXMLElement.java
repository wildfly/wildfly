/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.clustering.controller.xml.XMLElementReader;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Encapsulates an XML element for a subsystem resource.
 */
public interface ResourceXMLElement extends ResourceXMLContainer, XMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    /**
     * Creates an element whose attributes and content should be ignored when present.
     * @param name the qualified name of ignored element
     * @return an element whose attributes and content should be ignored when present.
     */
    static ResourceXMLElement ignore(QName name, XMLCardinality cardinality) {
        return new DefaultResourceXMLElement(name, cardinality, new XMLElementReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                ClusteringLogger.ROOT_LOGGER.elementIgnored(name);
                this.skipElement(reader);
            }

            private void skipElement(XMLExtendedStreamReader reader) throws XMLStreamException {
                while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
                    this.skipElement(reader);
                }
            }
        }, XMLContentWriter.empty(), Stability.DEFAULT);
    }

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
            Collection<AttributeDefinition> attributes = this.getAttributes();
            AttributeDefinitionXMLConfiguration configuration = this.getConfiguration();
            XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content = this.getContent();

            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader = new ResourceXMLContainerReader(new ResourceAttributesXMLContentReader(attributes, configuration), content);
            XMLContentWriter<ModelNode> writer = new ResourceXMLContainerWriter<>(this.name, new ResourceAttributesXMLContentWriter(attributes, configuration), Function.identity(), content) {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode content) throws XMLStreamException {
                    // Skip if empty
                    if (!this.isEmpty(content)) {
                        super.writeContent(writer, content);
                    }
                }
            };

            return new DefaultResourceXMLElement(this.name, this.getCardinality(), reader, writer, this.stability);
        }
    }

    class DefaultResourceXMLElement extends DefaultXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLElement {

        DefaultResourceXMLElement(QName name, XMLCardinality cardinality, XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader, XMLContentWriter<ModelNode> writer, Stability stability) {
            super(name, cardinality, reader, writer, stability);
        }
    }
}

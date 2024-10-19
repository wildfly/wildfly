/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Encapsulates an XML element for a subsystem resource.
 */
public interface ResourceXMLElement extends ResourceXMLContainer, XMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder extends ResourceXMLContainer.Builder<ResourceXMLElement, Builder> {
    }

    class DefaultBuilder extends ResourceXMLContainer.AbstractBuilder<ResourceXMLElement, Builder> implements Builder {
        private final QName name;

        DefaultBuilder(QName name, FeatureFilter filter, QNameResolver resolver) {
            this(name, filter, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
        }

        DefaultBuilder(QName name, FeatureFilter filter, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(filter, resolver, configuration);
            this.name = name;
        }

        @Override
        protected ResourceXMLElement.Builder builder() {
            return this;
        }

        @Override
        public ResourceXMLElement build() {
            return new DefaultResourceContentXMLElement(this.name, this.getCardinality(), this.getAttributes(), this.getConfiguration(), this.getContent());
        }
    }

    class DefaultResourceContentXMLElement extends DefaultXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLElement {

        DefaultResourceContentXMLElement(QName name, XMLCardinality cardinality, Collection<AttributeDefinition> attributes, AttributeDefinitionXMLConfiguration configuration, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content) {
            this(name, cardinality, new ResourceAttributesXMLContentReader(attributes, configuration), new ResourceAttributesXMLContentWriter(attributes, configuration), content);
        }

        private DefaultResourceContentXMLElement(QName name, XMLCardinality cardinality, XMLElementReader<ModelNode> attributesReader, XMLContentWriter<ModelNode> attributesWriter, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content) {
            super(name, cardinality, new ResourceXMLContainerReader(name, attributesReader, content), new ResourceXMLContainerWriter<>(name, attributesWriter, Function.identity(), content), Stability.DEFAULT);
        }
    }
}

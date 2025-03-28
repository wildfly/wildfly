/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLAll;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates a group of XML particles for a subsystem resource using xs:all (i.e. unordered) semantics.
 * @author Paul Ferraro
 */
public interface ResourceXMLAll extends ResourceXMLElementGroup, XMLAll<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder extends ResourceXMLElementGroup.Builder<ResourceXMLAll, Builder> {
    }

    class DefaultBuilder extends ResourceXMLElementGroup.AbstractBuilder<ResourceXMLAll, Builder> implements Builder {

        DefaultBuilder(FeatureRegistry registry, QNameResolver resolver) {
            this(registry, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
        }

        DefaultBuilder(FeatureRegistry registry, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(registry, resolver, configuration);
        }

        @Override
        public Builder addElement(ResourceXMLElement element) {
            if (element.getCardinality().isRepeatable()) {
                throw ClusteringLogger.ROOT_LOGGER.illegalXMLAllElementCardinality(element);
            }
            return super.addElement(element);
        }

        @Override
        public Builder withCardinality(XMLCardinality cardinality) {
            // https://www.w3.org/TR/xmlschema11-1/#sec-cos-all-limited
            if (cardinality.isRepeatable()) {
                throw ClusteringLogger.ROOT_LOGGER.illegalXMLCardinality(cardinality);
            }
            return super.withCardinality(cardinality);
        }

        @Override
        public ResourceXMLAll build() {
            return new DefaultResourceXMLAll(this.getElements(), this.getCardinality());
        }

        @Override
        protected Builder builder() {
            return this;
        }
    }

    class DefaultResourceXMLAll extends XMLAll.DefaultXMLAll<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLAll {

        DefaultResourceXMLAll(Collection<ResourceXMLElement> elements, XMLCardinality cardinality) {
            super(elements, cardinality);
        }
    }
}

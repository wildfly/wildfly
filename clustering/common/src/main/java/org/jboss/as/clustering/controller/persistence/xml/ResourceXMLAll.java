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
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.controller.FeatureFilter;
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

        DefaultBuilder(FeatureFilter filter, QNameResolver resolver) {
            this(filter, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
        }

        DefaultBuilder(FeatureFilter filter, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(filter, resolver, configuration);
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

        DefaultResourceXMLAll(Collection<XMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> elements, XMLCardinality cardinality) {
            super(elements, cardinality);
        }
    }
}

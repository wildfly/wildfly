/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLChoice;
import org.jboss.as.clustering.controller.xml.XMLParticleGroup;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates a group of XML particles for a subsystem resource using xs:choice (i.e. one of) semantics.
 * @author Paul Ferraro
 */
public interface ResourceXMLChoice extends ResourceXMLParticleGroup, XMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder extends ResourceXMLParticleGroup.Builder<ResourceXMLChoice, Builder> {
    }

    class DefaultBuilder extends ResourceXMLParticleGroup.AbstractBuilder<ResourceXMLChoice, Builder> implements Builder {

        DefaultBuilder(FeatureRegistry registry, QNameResolver resolver) {
            super(registry, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
        }

        @Override
        public ResourceXMLChoice build() {
            return new DefaultResourceContentXMLChoice(this.getGroups(), this.getCardinality());
        }

        @Override
        protected Builder builder() {
            return this;
        }
    }

    class DefaultResourceContentXMLChoice extends XMLChoice.DefaultXMLChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLChoice {

        DefaultResourceContentXMLChoice(Collection<XMLParticleGroup<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> groups, XMLCardinality cardinality) {
            super(groups, cardinality);
        }
    }
}

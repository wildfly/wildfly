/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLElementGroup;
import org.jboss.as.clustering.controller.xml.XMLParticle;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.Assert;

/**
 * Encapsulates an optional group of XML particles for one of more singleton resource registrations using xs:choice (i.e. one of) semantics.
 * @author Paul Ferraro
 */
public interface SingletonResourceRegistrationXMLChoice extends ResourceXMLChoice {

    interface Builder extends XMLElementGroup.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, SingletonResourceRegistrationXMLElement, SingletonResourceRegistrationXMLChoice, Builder> {
        /**
         * Overrides the key used to index any generated operation for the implied resource.
         * @param operationKey an operation key
         * @return a reference to this builder.
         */
        Builder withOperationKey(PathElement operationKey);
    }

    class DefaultBuilder extends XMLParticle.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, SingletonResourceRegistrationXMLChoice, Builder> implements Builder {
        private final List<ResourceRegistrationXMLElement> elements = new LinkedList<>();
        private final ResourceRegistration implied;
        private final FeatureFilter filter;
        private volatile PathElement operationKey;

        DefaultBuilder(ResourceRegistration implied, FeatureFilter filter) {
            super(XMLCardinality.Single.OPTIONAL, Predicate.not(XMLCardinality::isRequired));
            Assert.assertFalse(implied.getPathElement().isWildcard());
            this.implied = implied;
            this.filter = filter;
            this.operationKey = implied.getPathElement();
        }

        @Override
        public Builder addElement(SingletonResourceRegistrationXMLElement element) {
            if (this.filter.enables(element)) {
                this.elements.add(element);
            }
            return this;
        }

        @Override
        public Builder withOperationKey(PathElement operationKey) {
            this.operationKey = operationKey;
            return this;
        }

        @Override
        public SingletonResourceRegistrationXMLChoice build() {
            PathElement impliedPath = this.filter.enables(this.implied) ? this.implied.getPathElement() : null;
            PathElement operationKey = this.operationKey;
            return new DefaultSingletonResourceRegistrationXMLChoice(this.elements, this.getCardinality(), new Consumer<>() {
                @Override
                public void accept(Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) {
                    if (impliedPath != null) {
                        PathAddress parentOperationKey = context.getKey();
                        Map<PathAddress, ModelNode> operations = context.getValue();
                        // Create operation address from address of parent operation, since this may not be the same as the parent operation key
                        ModelNode parentOperation = (parentOperationKey.size() > 0) ? operations.get(parentOperationKey) : null;
                        PathAddress parentAddress = (parentOperation != null) ? PathAddress.pathAddress(parentOperation.get(ModelDescriptionConstants.OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
                        PathAddress operationAddress = parentAddress.append(impliedPath);
                        operations.put(parentOperationKey.append(operationKey), Util.createAddOperation(operationAddress));
                    }
                }
            }, this.implied.getStability());
        }

        @Override
        protected Builder builder() {
            return this;
        }
    }

    class DefaultSingletonResourceRegistrationXMLChoice extends DefaultXMLElementChoice<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements SingletonResourceRegistrationXMLChoice {

        DefaultSingletonResourceRegistrationXMLChoice(Collection<ResourceRegistrationXMLElement> elements, XMLCardinality cardinality, Consumer<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> absenteeHandler, Stability stability) {
            super(elements, cardinality, absenteeHandler, stability);
        }
    }
}

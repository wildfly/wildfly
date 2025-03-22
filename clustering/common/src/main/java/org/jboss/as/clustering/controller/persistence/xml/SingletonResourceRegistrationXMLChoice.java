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

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLElementGroup;
import org.jboss.as.clustering.controller.xml.XMLParticle;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates an group of XML elements for one of more singleton resource registrations using xs:choice (i.e. one of) semantics.
 * This choice may designate a {@link ResourceRegistration}, for which an empty {@link ModelDescriptionConstants#ADD} operation will be created if no choice is present.
 * @author Paul Ferraro
 */
public interface SingletonResourceRegistrationXMLChoice extends ResourceXMLChoice {

    interface Builder extends XMLElementGroup.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, SingletonResourceRegistrationXMLElement, SingletonResourceRegistrationXMLChoice, Builder> {
        /**
         * Indicates that this choice is optional, i.e. {@link XMLCardinality.Single#OPTIONAL},
         * and an empty {@link ModelDescriptionConstants#ADD} operation for the specified {@link ResourceRegistration} should be created when no choice is present.
         * @param implied an implied {@link ResourceRegistration} to be assumed when no choice is present.
         * @return a reference to this builder.
         * @throws IllegalArgumentException if the specified {@link ResourceRegistration} specifies a wildcard {@link PathElement}.
         */
        default Builder implyIfEmpty(ResourceRegistration implied) {
            return this.implyIfEmpty(implied, implied.getPathElement());
        }

        /**
         * Indicates that this choice is optional, i.e. {@link XMLCardinality.Single#OPTIONAL},
         * and that an empty {@link ModelDescriptionConstants#ADD} operation for the specified {@link ResourceRegistration} should be created when no choice is present.
         * @param implied an implied {@link ResourceRegistration} to be assumed when no choice is present.
         * @param the operation key of the implied operation
         * @return a reference to this builder.
         * @throws IllegalArgumentException if the specified {@link ResourceRegistration} specifies a wildcard {@link PathElement}.
         */
        Builder implyIfEmpty(ResourceRegistration implied, PathElement impliedOperationKey);
    }

    class DefaultBuilder extends XMLParticle.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, SingletonResourceRegistrationXMLChoice, Builder> implements Builder {
        private final List<ResourceRegistrationXMLElement> elements = new LinkedList<>();
        private final FeatureFilter filter;
        private volatile Map.Entry<ResourceRegistration, PathElement> implied = null;

        DefaultBuilder(FeatureFilter filter) {
            this.filter = filter;
        }

        @Override
        public Builder withCardinality(XMLCardinality cardinality) {
            if (cardinality.isRepeatable()) {
                // A repeatable choice would otherwise allow multiple add operations for the same resource
                throw ClusteringLogger.ROOT_LOGGER.illegalXMLCardinality(cardinality);
            }
            return super.withCardinality(cardinality);
        }

        @Override
        public Builder addElement(SingletonResourceRegistrationXMLElement element) {
            if (this.filter.enables(element) && element.getCardinality().isEnabled()) {
                this.elements.add(element);
            }
            return this;
        }

        @Override
        public Builder implyIfEmpty(ResourceRegistration implied, PathElement impliedOperationKey) {
            if (implied.getPathElement().isWildcard()) {
                throw ClusteringLogger.ROOT_LOGGER.wildcardPathNotAllowed(implied.getPathElement());
            }
            if (this.filter.enables(implied)) {
                this.implied = Map.entry(implied, impliedOperationKey);
            }
            return this.withCardinality(XMLCardinality.of(0, this.getCardinality().getMaxOccurs()));
        }

        @Override
        public SingletonResourceRegistrationXMLChoice build() {
            Map.Entry<ResourceRegistration, PathElement> implied = this.implied;
            return new DefaultSingletonResourceRegistrationXMLChoice(this.elements, this.getCardinality(), new Consumer<>() {
                @Override
                public void accept(Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) {
                    if (implied != null) {
                        PathAddress parentOperationKey = context.getKey();
                        Map<PathAddress, ModelNode> operations = context.getValue();
                        // Create operation address from address of parent operation, since this may not be the same as the parent operation key
                        ModelNode parentOperation = (parentOperationKey.size() > 0) ? operations.get(parentOperationKey) : null;
                        PathAddress parentAddress = (parentOperation != null) ? PathAddress.pathAddress(parentOperation.get(ModelDescriptionConstants.OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
                        PathAddress operationAddress = parentAddress.append(implied.getKey().getPathElement());
                        operations.put(parentOperationKey.append(implied.getValue()), Util.createAddOperation(operationAddress));
                    }
                }
            }, (implied != null) ? implied.getKey().getStability() : Stability.DEFAULT);
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

/**
 * A cardinal XML particle.
 */
public interface XMLCardinal {

    /**
     * Returns the cardinality of this XML particle.
     * @return the cardinality of this XML particle.
     */
    XMLCardinality getCardinality();

    interface Builder {
        /**
         * Configures this XML particle with the specified cardinality.
         * @param cardinality an XML cardinality
         * @return a reference to this configurator.
         */
        Builder withCardinality(XMLCardinality cardinality);
    }

    abstract class AbstractBuilder<B extends Builder> implements Builder, XMLCardinal {
        private volatile XMLCardinality cardinality = XMLCardinality.Single.REQUIRED;

        @Override
        public XMLCardinality getCardinality() {
            return this.cardinality;
        }

        @Override
        public B withCardinality(XMLCardinality cardinality) {
            this.cardinality = cardinality;
            return this.builder();
        }

        protected abstract B builder();
    }
}

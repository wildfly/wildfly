/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.OptionalInt;

/**
 * Defines the cardinality of an XML particle.
 */
public interface XMLCardinality {

    /**
     * Returns the minimum number of occurrences of this particle.
     * @return the minimum number of occurrences of this particle.
     */
    int getMinOccurs();

    /**
     * Returns the maximum number of occurrences of this particle.
     * @return the maximum number of occurrences of this particle.
     */
    OptionalInt getMaxOccurs();

    static XMLCardinality of(int minOccurs, OptionalInt maxOccurs) {
        return new XMLCardinality() {
            @Override
            public int getMinOccurs() {
                return minOccurs;
            }

            @Override
            public OptionalInt getMaxOccurs() {
                return maxOccurs;
            }
        };
    }

    /**
     * Cardinality of empty content.
     */
    XMLCardinality NONE = of(0, OptionalInt.of(0));

    /**
     * Cardinalities for single particles.
     */
    enum Single implements XMLCardinality {
        OPTIONAL(0),
        REQUIRED(1),
        ;
        private static final OptionalInt MAX = OptionalInt.of(1);
        private final int min;

        Single(int min) {
            this.min = min;
        }

        @Override
        public int getMinOccurs() {
            return this.min;
        }

        @Override
        public OptionalInt getMaxOccurs() {
            return MAX;
        }
    }

    /**
     * Common cardinalities for unbounded particles.
     */
    enum Unbounded implements XMLCardinality {
        OPTIONAL(0),
        REQUIRED(1),
        ;
        private final int min;

        Unbounded(int min) {
            this.min = min;
        }

        @Override
        public int getMinOccurs() {
            return this.min;
        }

        @Override
        public OptionalInt getMaxOccurs() {
            return OptionalInt.empty();
        }
    }
}

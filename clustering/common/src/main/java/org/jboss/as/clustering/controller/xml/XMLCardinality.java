/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.OptionalInt;
import java.util.function.Predicate;

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

    /**
     * Indicates whether or not the associated particle must occur, i.e. minOccurs &gt; 0
     * @return true, if the associated particle is required, false otherwise
     */
    default boolean isRequired() {
        return this.getMinOccurs() > 0;
    }

    /**
     * Indicates whether or not the associated particle can occur more than once, i.e. maxOccurs &gt; 1
     * @return true, if the associated particle is repeatable, false otherwise
     */
    default boolean isRepeatable() {
        return this.getMaxOccurs().orElse(Integer.MAX_VALUE) > 1;
    }

    /**
     * Indicates whether or not the associated particle may occur at all, i.e. maxOccurs &gt; 0
     * @return true, if the associated particle is enabled, false otherwise
     */
    default boolean isEnabled() {
        return this.getMaxOccurs().orElse(Integer.MAX_VALUE) > 0;
    }

    /**
     * Cardinality of a disabled particle.
     */
    XMLCardinality DISABLED = of(0, OptionalInt.of(0));

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
     * Cardinality validators.
     */
    enum Validator implements Predicate<XMLCardinality> {
        DEFAULT(),
        ALL() {
            @Override
            public boolean test(XMLCardinality cardinality) {
                // maxOccurs may not exceed 1
                // https://www.w3.org/TR/xmlschema11-1/#sec-cos-all-limited
                return DEFAULT.test(cardinality) && cardinality.getMaxOccurs().orElse(Integer.MAX_VALUE) <= 1;
            }
        },
        ;

        @Override
        public boolean test(XMLCardinality cardinality) {
            // minOccurs may not be negative
            // minOccurs may not exceed maxOccurs
            return (cardinality.getMinOccurs() >= 0) && (cardinality.getMinOccurs() <= cardinality.getMaxOccurs().orElse(Integer.MAX_VALUE));
        }
    }
}

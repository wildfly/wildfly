/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.Set;

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

        @Override
        public String toString() {
            return XMLCardinality.toString(this);
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

        @Override
        public String toString() {
            return XMLCardinality.toString(this);
        }
    }

    /**
     * Creates an XML cardinality with the specified minOccurs and maxOccurs that describes the number of permissible occurrences of a given XML particle.
     * @param minOccurs the minimum permissible occurrences of the associated particle
     * @param maxOccurs when present, the maximum permissible occurrences of the associated particle, otherwise unbounded
     * @return an XML cardinality
     */
    static XMLCardinality of(int minOccurs, OptionalInt maxOccurs) {
        if ((minOccurs < 0) || (minOccurs > maxOccurs.orElse(Integer.MAX_VALUE))) {
            throw new IllegalArgumentException(Integer.toString(minOccurs));
        }
        // Reuse existing instance, if possible
        Set<? extends XMLCardinality> candidates = maxOccurs.isEmpty() ? EnumSet.allOf(Unbounded.class) : (maxOccurs.equals(Single.MAX) ? EnumSet.allOf(Single.class) : Set.of());
        for (XMLCardinality candidate : candidates) {
            if (minOccurs == candidate.getMinOccurs()) return candidate;
        }
        return new XMLCardinality() {
            @Override
            public int getMinOccurs() {
                return minOccurs;
            }

            @Override
            public OptionalInt getMaxOccurs() {
                return maxOccurs;
            }

            @Override
            public String toString() {
                return XMLCardinality.toString(this);
            }
        };
    }

    static String toString(XMLCardinality cardinality) {
        return String.format("minOccurs=\"%d\" maxOccurs=\"%s\"", cardinality.getMinOccurs(), cardinality.getMaxOccurs().isPresent() ? cardinality.getMaxOccurs().getAsInt() : "unbounded");
    }
}

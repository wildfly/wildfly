/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.xml.namespace.QName;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;

/**
 * Encapsulates a group of XML elements, e.g. xs:choice, xs:all, xs:sequence.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLElementGroup<RC, WC> extends XMLParticle<RC, WC> {

    /**
     * Returns the set of qualified element names.
     * @return the set of qualified element names.
     */
    Set<QName> getReaderNames();

    /**
     * Returns the qualified element names of this element group.
     * @return the qualified element names of this element group.
     */
    default Collection<QName> getNames() {
        return this.getReaderNames();
    }

    interface Builder<RC, WC, E extends XMLElement<RC, WC>, T extends XMLElementGroup<RC, WC>, B extends Builder<RC, WC, E, T, B>> extends XMLParticle.Builder<RC, WC, T, B> {
        /**
         * Adds an element to this compositor.
         * @param element an element to add
         * @return a reference to this builder
         */
        B addElement(E element);
    }

    abstract class AbstractBuilder<RC, WC, E extends XMLElement<RC, WC>, T extends XMLElementGroup<RC, WC>, B extends Builder<RC, WC, E, T, B>> extends XMLParticle.AbstractBuilder<RC, WC, T, B> implements Builder<RC, WC, E, T, B>, FeatureRegistry {
        private final List<E> elements = new LinkedList<>();
        private final FeatureRegistry registry;

        protected AbstractBuilder(Predicate<XMLCardinality> cardinalityValidator, FeatureRegistry registry) {
            super(cardinalityValidator);
            this.registry = registry;
        }

        @Override
        public Stability getStability() {
            return this.registry.getStability();
        }

        @Override
        public B addElement(E element) {
            if (this.enables(element) && element.getCardinality().isEnabled()) {
                this.elements.add(element);
            }
            return this.builder();
        }

        protected List<E> getElements() {
            return this.elements;
        }
    }

    class DefaultXMLElementGroup<RC, WC> extends DefaultXMLParticle<RC, WC> implements XMLElementGroup<RC, WC> {
        private static <RC, WC> Stability maxStability(Collection<? extends XMLParticle<RC, WC>> particles) {
            // Determine max stability of choices
            Stability maxStability = null;
            for (XMLParticle<RC, WC> particle : particles) {
                Stability particleStability = particle.getStability();
                if ((maxStability == null) || !particleStability.enables(maxStability)) {
                    maxStability = particleStability;
                }
            }
            return (maxStability != null) ? maxStability : Stability.DEFAULT;
        }
        private final Set<QName> names;

        protected DefaultXMLElementGroup(Set<QName> names, Collection<? extends XMLParticle<RC, WC>> particles, XMLCardinality cardinality, XMLElementReader<RC> reader) {
            this(names, cardinality, reader, XMLContentWriter.composite(particles), maxStability(particles));
        }

        protected DefaultXMLElementGroup(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            super(cardinality, reader, writer, stability);
            this.names = names;
        }

        @Override
        public Set<QName> getReaderNames() {
            return this.names;
        }

        @Override
        public int hashCode() {
            return this.getNames().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof XMLElementGroup)) return false;
            XMLElementGroup<?, ?> group = (XMLElementGroup<?, ?>) object;
            return this.getNames().equals(group.getNames());
        }

        @Override
        public String toString() {
            return this.getNames().toString();
        }
    }
}

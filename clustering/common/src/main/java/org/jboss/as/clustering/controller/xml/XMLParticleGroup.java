/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;

/**
 * Encapsulates a group of XML particles, e.g. xs:choice, xs:sequence.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLParticleGroup<RC, WC> extends XMLElementGroup<RC, WC> {

    interface Builder<RC, WC, E extends XMLElement<RC, WC>, T extends XMLParticleGroup<RC, WC>, B extends Builder<RC, WC, E, T, B>> extends XMLElementGroup.Builder<RC, WC, E, T, B> {
        /**
         * Adds an XML choice to this group.
         * @param choice a choice of elements.
         * @return a reference to this builder
         */
        B addChoice(XMLChoice<RC, WC> choice);

        /**
         * Adds an XML sequence to this group.
         * @param sequence a sequence of elements.
         * @return a reference to this builder
         */
        B addSequence(XMLSequence<RC, WC> sequence);
    }

    abstract class AbstractBuilder<RC, WC, E extends XMLElement<RC, WC>, T extends XMLParticleGroup<RC, WC>, B extends Builder<RC, WC, E, T, B>> extends XMLParticle.AbstractBuilder<RC, WC, T, B> implements Builder<RC, WC, E, T, B>, FeatureRegistry {
        private final List<XMLParticleGroup<RC, WC>> groups = new LinkedList<>();
        private final FeatureRegistry registry;

        protected AbstractBuilder(FeatureRegistry registry) {
            this.registry = registry;
        }

        @Override
        public Stability getStability() {
            return this.registry.getStability();
        }

        @Override
        public B addElement(E element) {
            if (this.enables(element) && element.getCardinality().isEnabled()) {
                this.groups.add(new XMLChoice.DefaultXMLElementChoice<>(element));
            }
            return this.builder();
        }

        @Override
        public B addChoice(XMLChoice<RC, WC> choice) {
            return this.addGroup(choice);
        }

        @Override
        public B addSequence(XMLSequence<RC, WC> sequence) {
            return this.addGroup(sequence);
        }

        private B addGroup(XMLParticleGroup<RC, WC> group) {
            // Omit empty/disabled group
            if (!group.getReaderNames().isEmpty() && this.enables(group) && group.getCardinality().isEnabled()) {
                this.groups.add(group);
            }
            return this.builder();
        }

        protected List<XMLParticleGroup<RC, WC>> getGroups() {
            return this.groups;
        }
    }

    class DefaultXMLParticleGroup<RC, WC> extends DefaultXMLElementGroup<RC, WC> implements XMLParticleGroup<RC, WC> {

        protected DefaultXMLParticleGroup(Set<QName> names, Collection<? extends XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality, XMLElementReader<RC> reader) {
            super(names, groups, cardinality, reader);
        }

        protected DefaultXMLParticleGroup(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            super(names, cardinality, reader, writer, stability);
        }
    }
}

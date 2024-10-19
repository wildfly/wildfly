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
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Feature;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Encapsulates a group of XML elements, e.g. xs:choice, xs:all, xs:sequence.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLElementGroup<RC, WC> extends XMLParticle<RC, WC> {

    Set<QName> getNames();

    interface Builder<RC, WC, T extends XMLElementGroup<RC, WC>, B extends Builder<RC, WC, T, B>> extends XMLParticle.Builder<RC, WC, T, B> {
        /**
         * Adds an element to this compositor.
         * @param element an element to add
         * @return a reference to this builder
         */
        B addElement(XMLElement<RC, WC> element);
    }

    abstract class AbstractBuilder<RC, WC, T extends XMLElementGroup<RC, WC>, B extends Builder<RC, WC, T, B>> extends XMLParticle.AbstractBuilder<RC, WC, T, B> implements Builder<RC, WC, T, B>, FeatureFilter {
        protected static final Predicate<XMLCardinality> ALL_VALIDATOR = new Predicate<>() {
            @Override
            public boolean test(XMLCardinality cardinality) {
                // https://www.w3.org/TR/xmlschema11-1/#sec-cos-all-limited
                return (cardinality.getMinOccurs() <= 1) && (cardinality.getMaxOccurs().orElse(Integer.MAX_VALUE) <= 1);
            }
        };
        private final List<XMLElement<RC, WC>> elements = new LinkedList<>();
        private final FeatureFilter filter;

        protected AbstractBuilder(FeatureFilter filter) {
            super(ALL_VALIDATOR);
            this.filter = filter;
        }

        @Override
        public <F extends Feature> boolean enables(F feature) {
            return this.filter.enables(feature);
        }

        @Override
        public B addElement(XMLElement<RC, WC> element) {
            if (this.filter.enables(element)) {
                this.elements.add(element);
            }
            return this.builder();
        }

        protected List<XMLElement<RC, WC>> getElements() {
            return this.elements;
        }
    }

    class CompositeXMLContentWriter<C> implements XMLContentWriter<C> {
        private final Collection<? extends XMLParticle<?, C>> particles;

        CompositeXMLContentWriter(Collection<? extends XMLParticle<?, C>> particles) {
            this.particles = particles;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, C content) throws XMLStreamException {
            for (XMLParticle<?, C> particle : this.particles) {
                XMLContentWriter<C> contentWriter = particle.getWriter();
                if (!contentWriter.isEmpty(content)) {
                    contentWriter.writeContent(writer, content);
                }
            }
        }

        @Override
        public boolean isEmpty(C content) {
            for (XMLParticle<?, C> particle : this.particles) {
                XMLContentWriter<C> contentWriter = particle.getWriter();
                if (!contentWriter.isEmpty(content)) return false;
            }
            return true;
        }
    }

    class DefaultXMLElementGroup<RC, WC> extends DefaultXMLParticle<RC, WC> implements XMLElementGroup<RC, WC> {
        private final Set<QName> names;

        protected DefaultXMLElementGroup(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            super(cardinality, reader, writer, stability);
            this.names = names;
        }

        @Override
        public Set<QName> getNames() {
            return this.names;
        }

        @Override
        public int hashCode() {
            return this.getNames().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof XMLChoice)) return false;
            XMLChoice<?, ?> choice = (XMLChoice<?, ?>) object;
            return this.getNames().equals(choice.getNames());
        }

        @Override
        public String toString() {
            return this.getNames().toString();
        }
    }
}

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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;

/**
 * Encapsulates a group of XML particles, e.g. xs:choice, xs:sequence.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLParticleGroup<RC, WC> extends XMLElementGroup<RC, WC> {

    interface Builder<RC, WC, T extends XMLParticleGroup<RC, WC>, B extends Builder<RC, WC, T, B>> extends XMLElementGroup.Builder<RC, WC, T, B> {
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

    static <RC, WC> XMLParticleGroup<RC, WC> empty() {
        return new DefaultXMLParticleGroup<>(Set.of(), XMLCardinality.DISABLED, new XMLElementReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                throw ParseUtils.unexpectedElement(reader);
            }
        }, XMLContentWriter.empty(), Stability.DEFAULT);
    }

    static <RC, WC> XMLParticleGroup<RC, WC> singleton(XMLElement<RC, WC> element) {
        return new DefaultXMLParticleGroup<>(Set.of(element.getName()), element.getCardinality(), new XMLElementReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                // Validate entry criteria
                Assert.assertTrue(reader.isStartElement());
                int occurrences = 0;
                // Try matching w/out namespace for PersistentResourceXMLDescription compatiblity
                if (element.getName().equals(reader.getName()) || element.getName().getLocalPart().equals(reader.getLocalName())) {
                    int maxOccurs = element.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
                    do {
                        occurrences += 1;
                        if (occurrences > maxOccurs) {
                            throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, Set.of(element.getName()), element.getCardinality());
                        }
                        element.getReader().readElement(reader, context);
                    } while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT) && ((reader.getName().equals(element.getName()) || (reader.getLocalName().equals(element.getName().getLocalPart())))) && (occurrences < maxOccurs));
                }
                if (occurrences < element.getCardinality().getMinOccurs()) {
                    throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, Set.of(element.getName()), element.getCardinality());
                }
            }
        }, element.getWriter(), element.getStability());
    }

    abstract class AbstractBuilder<RC, WC, T extends XMLParticleGroup<RC, WC>, B extends Builder<RC, WC, T, B>> extends XMLParticle.AbstractBuilder<RC, WC, T, B> implements Builder<RC, WC, T, B>, FeatureRegistry {
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
        public B addElement(XMLElement<RC, WC> element) {
            if (this.enables(element) && element.getCardinality().isEnabled()) {
                this.groups.add(singleton(element));
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
            if (!group.getNames().isEmpty() && this.enables(group) && group.getCardinality().isEnabled()) {
                this.groups.add(group);
            }
            return this.builder();
        }

        protected List<XMLParticleGroup<RC, WC>> getGroups() {
            return this.groups;
        }
    }

    class DefaultXMLParticleGroup<RC, WC> extends DefaultXMLElementGroup<RC, WC> implements XMLParticleGroup<RC, WC> {

        protected DefaultXMLParticleGroup(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            this(names, names, cardinality, reader, writer, stability);
        }

        protected DefaultXMLParticleGroup(Set<QName> names, Collection<QName> allNames, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            super(names, allNames, cardinality, reader, writer, stability);
        }
    }
}

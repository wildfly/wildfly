/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;

/**
 * Encapsulates a group of XML particles with xs:sequence (i.e. ordered) semantics.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLSequence<RC, WC> extends XMLParticleGroup<RC, WC> {

    interface Builder<RC, WC> extends XMLParticleGroup.Builder<RC, WC, XMLSequence<RC, WC>, Builder<RC, WC>> {
    }

    class DefaultBuilder<RC, WC> extends XMLParticleGroup.AbstractBuilder<RC, WC, XMLSequence<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {
        DefaultBuilder(FeatureRegistry registry) {
            super(registry);
        }

        @Override
        public XMLSequence<RC, WC> build() {
            return new DefaultXMLSequence<>(this.getGroups(), this.getCardinality());
        }

        @Override
        protected Builder<RC, WC> builder() {
            return this;
        }
    }

    class DefaultXMLSequence<RC, WC> extends DefaultXMLParticleGroup<RC, WC> implements XMLSequence<RC, WC> {
        private static <RC, WC> Set<QName> collectNames(List<XMLParticleGroup<RC, WC>> groups) {
            Set<QName> names = new TreeSet<>(QNameResolver.COMPARATOR);
            for (XMLParticleGroup<RC, WC> group : groups) {
                names.addAll(group.getNames());
                // Look no further than first required particle of sequence
                if (group.getCardinality().isRequired()) {
                    break;
                }
            }
            return Collections.unmodifiableSet(names);
        }

        protected DefaultXMLSequence(List<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(collectNames(groups), Collections.unmodifiableList(groups), cardinality);
        }

        private DefaultXMLSequence(Set<QName> names, List<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(names, groups.stream().map(XMLParticleGroup::getAllNames).flatMap(Collection::stream).collect(Collectors.toUnmodifiableList()), cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    Iterator<XMLParticleGroup<RC, WC>> sequence = groups.iterator();
                    XMLParticleGroup<RC, WC> group = null;
                    int occurrences = 0;
                    do {
                        if (group != null) {
                            if (!group.getNames().contains(reader.getName())) {
                                if (occurrences < group.getCardinality().getMinOccurs()) {
                                    throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, group.getAllNames(), group.getCardinality());
                                }
                                group = this.findNext(reader, sequence);
                                occurrences = 0;
                            }
                        } else {
                            group = this.findNext(reader, sequence);
                        }
                        if (group != null) {
                            occurrences += 1;
                            // Validate maxOccurs
                            if (occurrences > group.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE)) {
                                throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, group.getAllNames(), group.getCardinality());
                            }
                            group.getReader().readElement(reader, context);
                        }
                    } while ((group != null) && (reader.getEventType() != XMLStreamConstants.END_ELEMENT));

                    // Verify that any remaining groups in sequence are optional
                    while (sequence.hasNext()) {
                        group = sequence.next();
                        if (group.getCardinality().isRequired()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, group.getAllNames(), group.getCardinality());
                        }
                    }
                }

                private XMLParticleGroup<RC, WC> findNext(XMLExtendedStreamReader reader, Iterator<XMLParticleGroup<RC, WC>> remaining) throws XMLStreamException {
                    while (remaining.hasNext()) {
                        XMLParticleGroup<RC, WC> group = remaining.next();
                        if (group.getNames().contains(reader.getName())) {
                            return group;
                        }
                        // Validate minOccurs
                        if (group.getCardinality().isRequired()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, group.getAllNames(), group.getCardinality());
                        }
                    }
                    return null;
                }
            }, new CompositeXMLContentWriter<>(groups));
        }

        protected DefaultXMLSequence(Set<QName> names, Collection<QName> allNames, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer) {
            super(names, allNames, cardinality, reader, writer, Stability.DEFAULT);
        }
    }
}

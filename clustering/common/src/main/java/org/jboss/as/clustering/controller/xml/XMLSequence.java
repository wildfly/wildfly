/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.controller.parsing.ParseUtils;
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
        DefaultBuilder(FeatureFilter filter) {
            super(filter);
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

        protected DefaultXMLSequence(List<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(new TreeSet<>(QNameResolver.COMPARATOR), groups, cardinality);
        }

        private DefaultXMLSequence(Set<QName> names, List<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(names, cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    Iterator<XMLParticleGroup<RC, WC>> sequence = groups.iterator();
                    XMLParticleGroup<RC, WC> current = XMLParticleGroup.empty();
                    XMLParticleGroup<RC, WC> match = null;
                    int occurrences = 0;
                    do {
                        // Iterate through sequence until we find a match
                        // Try matching w/out namespace for PersistentResourceXMLDescription compatibility
                        while (!current.getNames().contains(reader.getName()) && !current.getNames().contains(new QName(reader.getLocalName()))) {
                            // Validate minOccurs
                            if (occurrences < current.getCardinality().getMinOccurs()) {
                                throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, current.getNames(), current.getCardinality());
                            }
                            if (sequence.hasNext()) {
                                current = sequence.next();
                                occurrences = 0;
                            } else {
                                if ((match == null) && (cardinality.getMinOccurs() > 0)) {
                                    // If we did not match any elements, but should have
                                    throw ParseUtils.unexpectedElement(reader, names.stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                                }
                                // No more possible matches
                                return;
                            }
                        }
                        match = current;
                        occurrences += 1;
                        // Validate maxOccurs
                        if (occurrences > current.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE)) {
                            throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, current.getNames(), current.getCardinality());
                        }
                        current.getReader().readElement(reader, context);
                    } while (reader.getEventType() != XMLStreamConstants.END_ELEMENT);
                }
            }, new CompositeXMLContentWriter<>(groups));
            // Look no further than first required particle of sequence
            for (XMLParticleGroup<RC, WC> group : groups) {
                names.addAll(group.getNames());
                if (group.getCardinality().getMinOccurs() > 0) {
                    break;
                }
            }
        }

        protected DefaultXMLSequence(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer) {
            super(names, cardinality, reader, writer, Stability.DEFAULT);
        }
    }
}

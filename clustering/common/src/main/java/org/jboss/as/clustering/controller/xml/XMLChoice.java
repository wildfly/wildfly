/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
 * Encapsulates a group of XML particles using xs:choice (i.e. one of) semantics.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLChoice<RC, WC> extends XMLParticleGroup<RC, WC> {

    interface Builder<RC, WC> extends XMLParticleGroup.Builder<RC, WC, XMLChoice<RC, WC>, Builder<RC, WC>> {
    }

    class DefaultBuilder<RC, WC> extends XMLParticleGroup.AbstractBuilder<RC, WC, XMLChoice<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {
        DefaultBuilder(FeatureFilter filter) {
            super(filter);
        }

        @Override
        public XMLChoice<RC, WC> build() {
            return new DefaultXMLChoice<>(this.getGroups(), this.getCardinality());
        }

        @Override
        protected Builder<RC, WC> builder() {
            return this;
        }
    }

    class DefaultXMLChoice<RC, WC> extends DefaultXMLParticleGroup<RC, WC> implements XMLChoice<RC, WC> {

        protected DefaultXMLChoice(Collection<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(new TreeMap<>(QNameResolver.COMPARATOR), groups, cardinality);
        }

        private DefaultXMLChoice(Map<QName, XMLParticleGroup<RC, WC>> choices, Collection<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(choices.keySet(), cardinality, new XMLElementReader<>() {
                private XMLParticleGroup<RC, WC> findChoice(XMLExtendedStreamReader reader) {
                    XMLParticleGroup<RC, WC> choice = choices.get(reader.getName());
                    if (choice == null) {
                        // Match w/out namespace for PersistentResourceXMLDescription compatibility
                        choice = choices.get(new QName(reader.getLocalName()));
                    }
                    return choice;
                }

                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    Assert.assertTrue(reader.isStartElement());
                    int occurrences = 0;
                    XMLParticleGroup<RC, WC> choice = this.findChoice(reader);
                    if (choice != null) {
                        int maxOccurs = choice.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
                        do {
                            occurrences += 1;
                            // Validate maxOccurs
                            if (occurrences > maxOccurs) {
                                throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, choice.getNames(), choice.getCardinality());
                            }
                            choice.getReader().readElement(reader, context);
                        } while ((reader.getEventType() != XMLStreamConstants.END_ELEMENT) && (this.findChoice(reader) == choice) && (occurrences < maxOccurs));
                        // Validate minOccurs
                        if (occurrences < choice.getCardinality().getMinOccurs()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, choice.getNames(), choice.getCardinality());
                        }
                    } else if (cardinality.getMinOccurs() > 0) {
                        throw ParseUtils.unexpectedElement(reader, choices.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }
                }
            }, new CompositeXMLContentWriter<>(groups));
            for (XMLParticleGroup<RC, WC> group : groups) {
                for (QName name : group.getNames()) {
                    if (choices.put(name, group) != null) {
                        throw ClusteringLogger.ROOT_LOGGER.duplicateElements(name);
                    }
                }
            }
        }

        protected DefaultXMLChoice(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer) {
            this(names, cardinality, reader, writer, Stability.DEFAULT);
        }

        protected DefaultXMLChoice(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            super(names, cardinality, reader, writer, stability);
        }
    }
}

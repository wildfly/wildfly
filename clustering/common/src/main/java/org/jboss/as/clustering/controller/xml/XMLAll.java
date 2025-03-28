/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;

/**
 * Encapsulates a group of XML particles with xs:all (i.e. unordered) semantics.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer context
 */
public interface XMLAll<RC, WC> extends XMLElementGroup<RC, WC> {

    interface Builder<RC, WC> extends XMLElementGroup.Builder<RC, WC, XMLElement<RC, WC>, XMLAll<RC, WC>, Builder<RC, WC>> {
    }

    class DefaultBuilder<RC, WC> extends XMLElementGroup.AbstractBuilder<RC, WC, XMLElement<RC, WC>, XMLAll<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {

        DefaultBuilder(FeatureRegistry registry) {
            super(registry);
        }

        @Override
        public Builder<RC, WC> withCardinality(XMLCardinality cardinality) {
            // https://www.w3.org/TR/xmlschema11-1/#sec-cos-all-limited
            if (cardinality.isRepeatable()) {
                throw ClusteringLogger.ROOT_LOGGER.illegalXMLCardinality(cardinality);
            }
            return super.withCardinality(cardinality);
        }

        @Override
        public Builder<RC, WC> addElement(XMLElement<RC, WC> element) {
            if (element.getCardinality().isRepeatable()) {
                throw ClusteringLogger.ROOT_LOGGER.illegalXMLAllElementCardinality(element);
            }
            return super.addElement(element);
        }

        @Override
        public XMLAll<RC, WC> build() {
            return new DefaultXMLAll<>(this.getElements(), this.getCardinality());
        }

        @Override
        protected Builder<RC, WC> builder() {
            return this;
        }
    }

    class DefaultXMLAll<RC, WC> extends DefaultXMLElementGroup<RC, WC> implements XMLAll<RC, WC> {
        static <RC, WC> SortedMap<QName, XMLElement<RC, WC>> collect(Collection<? extends XMLElement<RC, WC>> elements) {
            SortedMap<QName, XMLElement<RC, WC>> result = new TreeMap<>(QNameResolver.COMPARATOR);
            for (XMLElement<RC, WC> element : elements) {
                if (result.put(element.getName(), element) != null) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicateElements(element.getName());
                }
            }
            return Collections.unmodifiableSortedMap(result);
        }

        protected DefaultXMLAll(Collection<? extends XMLElement<RC, WC>> elements, XMLCardinality cardinality) {
            this(collect(elements), Collections.unmodifiableCollection(elements), cardinality);
        }

        private DefaultXMLAll(SortedMap<QName, XMLElement<RC, WC>> elements, Collection<XMLElement<RC, WC>> orderedElements, XMLCardinality cardinality) {
            super(elements.keySet(), orderedElements, cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    // Track occurrences via map removal since xs:all elements may never occur more than once
                    Map<QName, XMLElement<RC, WC>> remaining = new TreeMap<>(elements);

                    if (remaining.containsKey(reader.getName())) {
                        do {
                            remaining.remove(reader.getName()).getReader().readElement(reader, context);
                        } while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT) && remaining.containsKey(reader.getName()));
                    } else if (cardinality.isRequired()) {
                        throw ParseUtils.unexpectedElement(reader, elements.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }

                    if (!remaining.isEmpty()) {
                        // Validate that any remaining elements are optional
                        Set<QName> required = new TreeSet<>(QNameResolver.COMPARATOR);
                        for (XMLElement<RC, WC> remainingElement : remaining.values()) {
                            if (remainingElement.getCardinality().isRequired()) {
                                required.add(remainingElement.getName());
                            }
                        }
                        if (!required.isEmpty()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, required, XMLCardinality.Single.REQUIRED);
                        }
                        for (XMLElement<RC, WC> remainingElement : remaining.values()) {
                            remainingElement.getReader().handleAbsentElement(context);
                        }
                    }
                }

                @Override
                public void handleAbsentElement(RC context) {
                    for (XMLElement<RC, WC> element : orderedElements) {
                        element.getReader().handleAbsentElement(context);
                    }
                }
            });
        }
    }
}

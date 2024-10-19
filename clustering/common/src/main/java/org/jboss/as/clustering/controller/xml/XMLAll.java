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
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;

/**
 * Encapsulates a group of XML particles with xs:all (i.e. unordered) semantics.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer context
 */
public interface XMLAll<RC, WC> extends XMLElementGroup<RC, WC> {

    interface Builder<RC, WC> extends XMLElementGroup.Builder<RC, WC, XMLAll<RC, WC>, Builder<RC, WC>> {
    }

    class DefaultBuilder<RC, WC> extends XMLElementGroup.AbstractBuilder<RC, WC, XMLAll<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {

        DefaultBuilder(FeatureRegistry registry) {
            super(XMLCardinality.Validator.ALL, registry);
        }

        @Override
        public Builder<RC, WC> addElement(XMLElement<RC, WC> element) {
            if (element.getCardinality().isRepeatable()) {
                throw new IllegalArgumentException(element.getName().getLocalPart());
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
        static <RC, WC> SortedMap<QName, XMLElement<RC, WC>> collect(Collection<XMLElement<RC, WC>> elements) {
            SortedMap<QName, XMLElement<RC, WC>> result = new TreeMap<>(QNameResolver.COMPARATOR);
            for (XMLElement<RC, WC> element : elements) {
                if (result.put(element.getName(), element) != null) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicateElements(element.getName());
                }
            }
            return Collections.unmodifiableSortedMap(result);
        }

        protected DefaultXMLAll(Collection<XMLElement<RC, WC>> elements, XMLCardinality cardinality) {
            this(collect(elements), Collections.unmodifiableCollection(elements), cardinality);
        }

        private DefaultXMLAll(SortedMap<QName, XMLElement<RC, WC>> elements, Collection<XMLElement<RC, WC>> orderedElements, XMLCardinality cardinality) {
            this(elements.keySet(), cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    // Track occurrences via removal since xs:all elements may not occur more than once
                    Map<QName, XMLElement<RC, WC>> remaining = new TreeMap<>(elements);

                    XMLElement<RC, WC> element = null;
                    do {
                        element = remaining.remove(reader.getName());
                        if (element != null) {
                            element.getReader().readElement(reader, context);
                        }
                    } while ((element != null) && reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT);

                    if (!remaining.isEmpty()) {
                        // If all elements are remaining, validate that group is optional
                        // Otherwise, validate that any remaining elements are optional
                        if (remaining.size() != elements.size()) {
                            Set<QName> required = new TreeSet<>(QNameResolver.COMPARATOR);
                            for (XMLElement<RC, WC> remainingElement : remaining.values()) {
                                if (remainingElement.getCardinality().isRequired()) {
                                    required.add(remainingElement.getName());
                                }
                            }
                            if (!required.isEmpty()) {
                                throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, required, XMLCardinality.Single.REQUIRED);
                            }
                        } else if (cardinality.isRequired()) {
                            throw ParseUtils.unexpectedElement(reader, elements.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                        }
                    }
                }
            }, new CompositeXMLContentWriter<>(orderedElements));
        }

        protected DefaultXMLAll(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer) {
            super(names, names, cardinality, reader, writer, Stability.DEFAULT);
        }
    }
}

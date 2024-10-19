/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureFilter;
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
        DefaultBuilder(FeatureFilter filter) {
            super(filter);
        }

        @Override
        public Builder<RC, WC> addElement(XMLElement<RC, WC> element) {
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

        protected DefaultXMLAll(Collection<XMLElement<RC, WC>> elements, XMLCardinality cardinality) {
            this(new TreeSet<>(QNameResolver.COMPARATOR), elements, cardinality);
        }

        private DefaultXMLAll(Set<QName> names, Collection<XMLElement<RC, WC>> elements, XMLCardinality cardinality) {
            this(names, cardinality, new XMLElementReader<>() {
                private XMLElement<RC, WC> findElement(XMLExtendedStreamReader reader, Map<QName, XMLElement<RC, WC>> elements) {
                    XMLElement<RC, WC> element = elements.remove(reader.getName());
                    if (element == null) {
                        // Match w/out namespace for PersistentResourceXMLDescription compatibility
                        element = elements.remove(new QName(reader.getLocalName()));
                    }
                    return element;
                }

                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    // Track occurrences via set removal since xs:all elements may not occur more than once
                    Map<QName, XMLElement<RC, WC>> remainingElements = new TreeMap<>(QNameResolver.COMPARATOR);
                    for (XMLElement<RC, WC> element : elements) {
                        remainingElements.put(element.getName(), element);
                    }
                    XMLElement<RC, WC> element = null;
                    do {
                        element = this.findElement(reader, remainingElements);
                        if (element != null) {
                            element.getReader().readElement(reader, context);
                        }
                    } while ((element != null) && reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT);
                    // Validate that any remaining elements are optional
                    if (!remainingElements.isEmpty()) {
                        Set<QName> required = new TreeSet<>(QNameResolver.COMPARATOR);
                        for (XMLElement<RC, WC> remainingElement : remainingElements.values()) {
                            if (remainingElement.getCardinality().getMinOccurs() > 0) {
                                required.add(remainingElement.getName());
                            }
                        }
                        if (!required.isEmpty()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, required, XMLCardinality.Single.REQUIRED);
                        }
                    }
                }
            }, new CompositeXMLContentWriter<>(elements));
            for (XMLElement<RC, WC> element : elements) {
                if (!names.add(element.getName())) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicateElements(element.getName());
                }
            }
        }

        protected DefaultXMLAll(Set<QName> names, XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer) {
            super(names, cardinality, reader, writer, Stability.DEFAULT);
        }
    }
}

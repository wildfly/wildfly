/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;

/**
 * Encapsulates the content of an XML element.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLContent<RC, WC> extends XMLContentWriter<WC> {

    /**
     * Reads element content into the specified reader context.
     * @param reader a StaX reader
     * @param context the reader context
     * @throws XMLStreamException if the input could not be read from the specified reader.
     */
    void readContent(XMLExtendedStreamReader reader, RC context) throws XMLStreamException;

    /**
     * Returns an empty content
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @return an empty content
     */
    static <RC, WC> XMLContent<RC, WC> empty() {
        return new XMLContent<>() {
            @Override
            public void readContent(XMLExtendedStreamReader reader, RC value) throws XMLStreamException {
                Assert.assertTrue(reader.isStartElement());
                ParseUtils.requireNoContent(reader);
            }

            @Override
            public boolean isEmpty(WC content) {
                return true;
            }

            @Override
            public void writeContent(XMLExtendedStreamWriter streamWriter, WC value) throws XMLStreamException {
                // Do nothing
            }
        };
    }

    /**
     * Returns an empty content
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @return an empty content
     */
    static <RC, WC> XMLContent<RC, WC> of(XMLElementGroup<RC, WC> group) {
        return !group.getNames().isEmpty() ? new DefaultXMLContent<>(group) : empty();
    }

    class DefaultXMLContent<RC, WC> implements XMLContent<RC, WC> {
        private final XMLElementGroup<RC, WC> group;

        DefaultXMLContent(XMLElementGroup<RC, WC> group) {
            this.group = group;
        }

        @Override
        public void readContent(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
            // Validate entry criteria
            Assert.assertTrue(reader.isStartElement());
            QName parentElementName = reader.getName();
            int occurrences = 0;
            int maxOccurs = this.group.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
            // Do nested elements exist?
            if (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                // Read all nested elements
                do {
                    if (!this.group.getNames().contains(reader.getName())) {
                        throw ParseUtils.unexpectedElement(reader, this.group.getNames().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }
                    occurrences += 1;
                    // Validate maxOccurs
                    if (occurrences > maxOccurs) {
                        throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, this.group.getAllNames(), this.group.getCardinality());
                    }
                    // Consumes 1 or more elements
                    this.group.getReader().readElement(reader, context);
                } while (reader.getEventType() != XMLStreamConstants.END_ELEMENT);
            }
            // Validate minOccurs
            if (occurrences < this.group.getCardinality().getMinOccurs()) {
                throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, this.group.getAllNames(), this.group.getCardinality());
            }
            // Validate exit criteria
            if (!reader.isEndElement()) {
                throw ParseUtils.unexpectedElement(reader);
            }
            if (!reader.getName().equals(parentElementName)) {
                throw ParseUtils.unexpectedEndElement(reader);
            }
        }

        @Override
        public boolean isEmpty(WC content) {
            return this.group.getWriter().isEmpty(content);
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, WC content) throws XMLStreamException {
            this.group.getWriter().writeContent(writer, content);
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A readable/writable XML choice.
 * @param <RC> the reader context
 * @param <WC> the writer content
 * @author Paul Ferraro
 */
public interface XMLChoice<RC, WC> extends XMLContentWriter<WC> {

    /**
     * Returns the set of qualified choice names.
     * @return the set of qualified choice names.
     */
    Set<QName> getChoices();

    /**
     * Returns a reader for the specified qualified element name.
     * @return a reader for the specified qualified element name.
     */
    XMLContentReader<RC> getReader(QName name);

    /**
     * Returns the cardinality of this choice.
     * @return a cardinality
     */
    XMLCardinality getCardinality();

    /**
     * Returns an empty choice, i.e. with zero choices/elements.
     * @param <RC> the reader context
     * @param <WC> the writer content
     */
    static <RC, WC> XMLChoice<RC, WC> empty() {
        return new DefaultXMLChoice<>(Map.of(), XMLContent.empty(), XMLCardinality.NONE);
    }

    /**
     * Composes a xs:choice of the specified choices.
     * @param <RC> the reader context
     * @param <WC> the writer content
     * @param choices a collection of zero or more choices
     * @return a choice that reads/writes one of the specified choices.
     */
    static <RC, WC> XMLChoice<RC, WC> of(Collection<? extends XMLChoice<RC, WC>> choices, XMLCardinality cardinality) {
        if (choices.isEmpty()) return empty();

        Map<QName, XMLContentReader<RC>> readers = new HashMap<>();
        for (XMLChoice<RC, WC> choice : choices) {
            for (QName name : choice.getChoices()) {
                XMLContentReader<RC> existing = readers.put(name, choice.getReader(name));
                if (existing != null) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicateElements(name);
                }
            }
        }
        return new DefaultXMLChoice<>(readers, XMLContentWriter.of(choices), cardinality);
    }

    class DefaultXMLChoice<RC, WC> extends AbstractXMLChoice<RC, WC> {
        private final Map<QName, XMLContentReader<RC>> readers;
        private final XMLContentWriter<WC> writer;
        private final XMLCardinality cardinality;

        protected DefaultXMLChoice(Map<QName, XMLContentReader<RC>> readers, XMLContentWriter<WC> writer, XMLCardinality cardinality) {
            this.readers = readers;
            this.writer = writer;
            this.cardinality = cardinality;
        }

        @Override
        public Set<QName> getChoices() {
            return this.readers.keySet();
        }

        @Override
        public XMLContentReader<RC> getReader(QName name) {
            return this.readers.get(name);
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, WC content) throws XMLStreamException {
            this.writer.writeContent(writer, content);
        }

        @Override
        public boolean isEmpty(WC content) {
            return this.writer.isEmpty(content);
        }

        @Override
        public XMLCardinality getCardinality() {
            return this.cardinality;
        }
    }

    abstract class AbstractXMLChoice<RC, WC> implements XMLChoice<RC, WC> {

        @Override
        public int hashCode() {
            return this.getChoices().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof XMLChoice)) return false;
            XMLChoice<?, ?> choice = (XMLChoice<?, ?>) object;
            return this.getChoices().equals(choice.getChoices());
        }

        @Override
        public String toString() {
            return this.getChoices().toString();
        }
    }
}

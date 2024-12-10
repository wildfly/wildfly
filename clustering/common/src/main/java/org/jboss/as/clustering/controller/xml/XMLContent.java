/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A readable/writable model group of XML content.
 * @param <RC> the reader context
 * @param <WC> the writer content
 * @author Paul Ferraro
 */
public interface XMLContent<RC, WC> extends XMLContentReader<RC>, XMLContentWriter<WC> {

    /**
     * Returns an empty content
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @return an empty content
     */
    static <RC, WC> XMLContent<RC, WC> empty() {
        return of(XMLContentReader.empty(), XMLContentWriter.empty());
    }

    /**
     * Returns an empty content
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @return an empty content
     */
    static <RC, WC> XMLContent<RC, WC> of(XMLContentReader<RC> contentReader, XMLContentWriter<WC> contentWriter) {
        return new XMLContent<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                contentReader.readElement(reader, context);
            }

            @Override
            public XMLCardinality getCardinality() {
                return contentReader.getCardinality();
            }

            @Override
            public void writeContent(XMLExtendedStreamWriter writer, WC content) throws XMLStreamException {
                contentWriter.writeContent(writer, content);
            }

            @Override
            public boolean isEmpty(WC content) {
                return contentWriter.isEmpty(content);
            }
        };
    }

    /**
     * Composes xs:all content that reads/writes the specified choices in any order.
     * @param choices a list of zero or more choices
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @return content that reads/writes the specified choices in any order.
     */
    static <RC, WC> XMLContent<RC, WC> all(Collection<? extends XMLChoice<RC, WC>> all) {
        if (all.isEmpty()) return empty();

        Map<QName, XMLChoice<RC, WC>> choices = new HashMap<>();
        for (XMLChoice<RC, WC> choice : all) {
            for (QName name : choice.getChoices()) {
                XMLChoice<RC, WC> existing = choices.put(name, choice);
                if (existing != null) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicateElements(name);
                }
            }
        }
        XMLContentReader<RC> reader = new XMLContentReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                Map<XMLChoice<RC, WC>, AtomicInteger> occurrences = new HashMap<>();
                for (XMLChoice<RC, WC> choice : all) {
                    occurrences.put(choice, new AtomicInteger(0));
                }
                while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    QName name = reader.getName();
                    XMLChoice<RC, WC> choice = choices.get(name);
                    if (choice == null) {
                        // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
                        name = new QName(reader.getLocalName());
                        choice = choices.get(name);
                        if (choice == null) {
                            throw ParseUtils.unexpectedElement(reader, choices.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                        }
                    }
                    AtomicInteger occurrence = occurrences.get(choice);
                    // Validate maxOccurs
                    OptionalInt maxOccurs = choice.getCardinality().getMaxOccurs();
                    if (maxOccurs.isPresent() && (occurrence.getPlain() >= maxOccurs.getAsInt())) {
                        throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, choice.getChoices());
                    }
                    occurrence.setPlain(occurrence.getPlain() + 1);
                    choice.getReader(name).readElement(reader, context);
                }
                // Validate minOccurs
                for (Map.Entry<XMLChoice<RC, WC>, AtomicInteger> entry : occurrences.entrySet()) {
                    XMLChoice<RC, WC> choice = entry.getKey();
                    AtomicInteger occurrence = entry.getValue();
                    if (occurrence.getPlain() < choice.getCardinality().getMinOccurs()) {
                        throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, choice.getChoices());
                    }
                }
            }

            @Override
            public XMLCardinality getCardinality() {
                return XMLCardinality.Single.OPTIONAL;
            }
        };
        return of(reader, XMLContentWriter.of(all));
    }

    /**
     * Composes xs:sequence content that reads/writes the specified sequence of choices.
     * @param sequence a list of zero or more choices
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @return content that reads/writes the specified choices in sequential order.
     */
    static <RC, WC> XMLContent<RC, WC> sequence(List<? extends XMLChoice<RC, WC>> sequence) {
        if (sequence.isEmpty()) return empty();

        XMLContentReader<RC> reader = new XMLContentReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                Iterator<? extends XMLChoice<RC, WC>> choices = sequence.iterator();
                XMLChoice<RC, WC> currentChoice = XMLChoice.empty();
                int occurrences = 0;
                while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    QName name = reader.getName();
                    while (!currentChoice.getChoices().contains(name) && choices.hasNext()) {
                        // Validate minOccurs
                        if (occurrences < currentChoice.getCardinality().getMinOccurs()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, currentChoice.getChoices());
                        }
                        currentChoice = choices.next();
                        occurrences = 0;
                    }
                    if (!choices.hasNext()) {
                        throw ParseUtils.unexpectedElement(reader, currentChoice.getChoices().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }
                    XMLElementReader<RC> choiceReader = currentChoice.getReader(name);
                    if (choiceReader == null) {
                        throw ParseUtils.unexpectedElement(reader, currentChoice.getChoices().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    }
                    occurrences += 1;
                    OptionalInt maxOccurs = currentChoice.getCardinality().getMaxOccurs();
                    // Validate maxOccurs
                    if (maxOccurs.isPresent() && (occurrences > maxOccurs.getAsInt())) {
                        throw ClusteringLogger.ROOT_LOGGER.maxOccursExceeded(reader, currentChoice.getChoices());
                    }
                    choiceReader.readElement(reader, context);
                }
            }

            @Override
            public XMLCardinality getCardinality() {
                return XMLCardinality.Single.OPTIONAL;
            }
        };
        return of(reader, XMLContentWriter.of(sequence));
    }
}

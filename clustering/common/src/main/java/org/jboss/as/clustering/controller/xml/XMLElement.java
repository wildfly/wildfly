/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A readable/writable XML element, implemented as a singleton choice.
 * @param <RC> the reader context
 * @param <WC> the writer content
 * @author Paul Ferraro
 */
public interface XMLElement<RC, WC> extends XMLChoice<RC, WC>, XMLContent<RC, WC> {
    /**
     * The qualified name of this element.
     * @return a qualified name
     */
    QName getName();

    @Override
    default Set<QName> getChoices() {
        return Set.of(this.getName());
    }

    @Override
    default XMLContentReader<RC> getReader(QName name) {
        return this.getName().equals(name) ? this : null;
    }

    /**
     * Creates an element whose content should be ignored, if present.
     * @param <RC> the reader context
     * @param <WC> the writer content
     * @param name the qualified name of ignored element
     * @return an element whose content should be ignored.
     */
    static <RC, WC> XMLElement<RC, WC> ignore(QName name) {
        XMLContentReader<RC> reader = new XMLContentReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                if (!reader.isStartElement() || !reader.getName().equals(name)) {
                    throw ParseUtils.unexpectedElement(reader, Set.of(name.getLocalPart()));
                }
                ClusteringLogger.ROOT_LOGGER.elementIgnored(name);
                this.skipElement(reader);
                if (!reader.isEndElement() || !reader.getName().equals(name)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            private void skipElement(XMLExtendedStreamReader reader) throws XMLStreamException {
                while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
                    this.skipElement(reader);
                }
            }

            @Override
            public XMLCardinality getCardinality() {
                return XMLCardinality.Unbounded.OPTIONAL;
            }
        };
        return new DefaultXMLElement<>(name, reader, XMLContentWriter.empty());
    }

    /**
     * Applies an element wrapper to the specified content.
     * @param <RC> the reader context
     * @param <WC> the writer content
     * @param name the qualified name of the wrapper element
     * @param choice the XML content to wrap
     * @return an element that reads/writes the wrapped content.
     */
    static <RC, WC> XMLElement<RC, WC> wrap(QName name, XMLChoice<RC, WC> choice) {
        return wrap(name, XMLContent.all(List.of(choice)), choice.getCardinality());
    }

    /**
     * Applies an element wrapper to the specified content.
     * @param <RC> the reader context
     * @param <WC> the writer content
     * @param name the qualified name of the wrapper element
     * @param choice the XML content to wrap
     * @return an element that reads/writes the wrapped content.
     */
    static <RC, WC> XMLElement<RC, WC> wrap(QName name, XMLContent<RC, WC> content, XMLCardinality cardinality) {
        XMLContentReader<RC> reader = new XMLContentReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                // Validate entry criteria
                // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
                if (!reader.isStartElement() || (!reader.getName().equals(name) && !reader.getLocalName().equals(name.getLocalPart()))) {
                    throw ParseUtils.unexpectedElement(reader, Set.of(name.getLocalPart()));
                }
                ParseUtils.requireNoAttributes(reader);
                content.readElement(reader, context);
                // Validate exit criteria
                // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
                if (!reader.isEndElement() || (!reader.getName().equals(name) && !reader.getLocalName().equals(name.getLocalPart()))) {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            @Override
            public XMLCardinality getCardinality() {
                return cardinality;
            }
        };
        XMLContentWriter<WC> writer = new XMLContentWriter<>() {
            @Override
            public void writeContent(XMLExtendedStreamWriter writer, WC value) throws XMLStreamException {
                if (name.getNamespaceURI() != XMLConstants.NULL_NS_URI) {
                    writer.writeStartElement(name.getNamespaceURI(), name.getLocalPart());
                } else {
                    // PersistentResourceXMLDescription compatibility
                    writer.writeStartElement(name.getLocalPart());
                }
                content.writeContent(writer, value);
                writer.writeEndElement();
            }

            @Override
            public boolean isEmpty(WC value) {
                return content.isEmpty(value);
            }
        };
        return new DefaultXMLElement<>(name, reader, writer);
    }

    class DefaultXMLElement<RC, WC> extends AbstractXMLElement<RC, WC> {
        private final QName name;
        private final XMLContentReader<RC> reader;
        private final XMLContentWriter<WC> writer;

        protected DefaultXMLElement(QName name, XMLContentReader<RC> reader, XMLContentWriter<WC> writer) {
            this.name = name;
            this.reader = reader;
            this.writer = writer;
        }

        @Override
        public QName getName() {
            return this.name;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, RC value) throws XMLStreamException {
            this.reader.readElement(reader, value);
        }

        @Override
        public XMLCardinality getCardinality() {
            return this.reader.getCardinality();
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, WC content) throws XMLStreamException {
            this.writer.writeContent(writer, content);
        }

        @Override
        public boolean isEmpty(WC content) {
            return this.writer.isEmpty(content);
        }
    }

    abstract class AbstractXMLElement<RC, WC> implements XMLElement<RC, WC> {

        @Override
        public int hashCode() {
            return this.getName().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof XMLElement)) return false;
            XMLElement<?, ?> element = (XMLElement<?, ?>) object;
            return this.getName().equals(element.getName());
        }

        @Override
        public String toString() {
            return this.getName().toString();
        }
    }
}

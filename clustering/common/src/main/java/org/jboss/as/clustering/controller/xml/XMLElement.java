/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLAttributeReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;

/**
 * Encapsulates an XML element.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLElement<RC, WC> extends XMLContainer<RC, WC> {
    /**
     * The qualified name of this element.
     * @return a qualified name
     */
    QName getName();

    /**
     * Builder of an XML element.
     * @param <RC> the reader context
     * @param <WC> the writer content
     */
    interface Builder<RC, WC> extends XMLContainer.Builder<RC, WC, XMLElement<RC, WC>, Builder<RC, WC>> {
        /**
         * Adds readers and writer for the attributes of this element.
         * @param readers a map of readers per qualified name
         * @param writer a writer of the attributes of this element
         * @return a reference to this builder
         */
        Builder<RC, WC> withAttributes(Map<QName, XMLAttributeReader<RC>> readers, XMLContentWriter<WC> writer);
    }

    /**
     * Creates an element whose content should be ignored, if present.
     * @param <RC> the reader context
     * @param <WC> the writer content
     * @param name the qualified name of ignored element
     * @return an element whose content should be ignored.
     */
    static <RC, WC> XMLElement<RC, WC> ignore(QName name, XMLCardinality cardinality) {
        return new DefaultXMLElement<>(name, cardinality, new XMLElementReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                ClusteringLogger.ROOT_LOGGER.elementIgnored(name);
                this.skipElement(reader);
            }

            private void skipElement(XMLExtendedStreamReader reader) throws XMLStreamException {
                while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
                    this.skipElement(reader);
                }
            }
        }, XMLContentWriter.empty(), Stability.DEFAULT);
    }

    class DefaultBuilder<RC, WC> extends XMLContainer.AbstractBuilder<RC, WC, XMLElement<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {
        private final QName name;
        private volatile Map<QName, XMLAttributeReader<RC>> attributeReaders = Map.of();
        private volatile XMLContentWriter<WC> attributesWriter = XMLContentWriter.empty();
        private final Stability stability;

        DefaultBuilder(QName name, Stability stability) {
            this.name = name;
            this.stability = stability;
        }

        @Override
        public Builder<RC, WC> withAttributes(Map<QName, XMLAttributeReader<RC>> readers, XMLContentWriter<WC> writer) {
            if (this.attributeReaders.isEmpty()) {
                this.attributeReaders = new TreeMap<>(QNameResolver.COMPARATOR);
            }
            this.attributeReaders.putAll(readers);
            this.attributesWriter = this.attributesWriter.andThen(writer);
            return this;
        }

        @Override
        protected XMLElement.Builder<RC, WC> builder() {
            return this;
        }

        @Override
        public XMLElement<RC, WC> build() {
            QName name = this.name;
            Map<QName, XMLAttributeReader<RC>> attributeReaders = this.attributeReaders;
            XMLContentWriter<WC> attributesWriter = this.attributesWriter;
            XMLContent<RC, WC> content = this.getContent();
            XMLElementReader<RC> reader = new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    for (int i = 0; i < reader.getAttributeCount(); ++i) {
                        QName attributeName = reader.getAttributeName(i);
                        if (attributeName.getNamespaceURI().equals(XMLConstants.NULL_NS_URI) && !reader.getName().getNamespaceURI().equals(XMLConstants.NULL_NS_URI)) {
                            // Inherit namespace of element, if unspecified
                            attributeName = new QName(reader.getName().getNamespaceURI(), name.getLocalPart());
                        }
                        XMLAttributeReader<RC> attributeReader = attributeReaders.get(attributeName);
                        if (attributeReader == null) {
                            throw ParseUtils.unexpectedAttribute(reader, i, attributeReaders.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                        }
                        attributeReader.readAttribute(reader, i, context);
                    }
                    content.readContent(reader, context);
                }
            };
            XMLContentWriter<WC> writer = new XMLContentWriter<>() {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, WC value) throws XMLStreamException {
                    writer.writeStartElement(name.getNamespaceURI(), name.getLocalPart());
                    attributesWriter.writeContent(writer, value);
                    content.writeContent(writer, value);
                    writer.writeEndElement();
                }

                @Override
                public boolean isEmpty(WC value) {
                    return content.isEmpty(value);
                }
            };
            return new DefaultXMLElement<>(this.name, this.getCardinality(), reader, writer, this.stability);
        }
    }

    class DefaultXMLElement<RC, WC> extends DefaultXMLParticle<RC, WC> implements XMLElement<RC, WC> {
        private final QName name;

        protected DefaultXMLElement(QName name, XMLCardinality cardinality, XMLElementReader<RC> elementReader, XMLContentWriter<WC> elementWriter, Stability stability) {
            super(cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC value) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    if (!reader.getName().equals(name)) {
                        throw ParseUtils.unexpectedElement(reader, Set.of(name.getLocalPart()));
                    }
                    elementReader.readElement(reader, value);
                    // Validate exit criteria
                    if (!reader.isEndElement()) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    if (!reader.getName().equals(name)) {
                        throw ParseUtils.unexpectedEndElement(reader);
                    }
                }
            }, elementWriter, stability);
            this.name = name;
        }

        @Override
        public QName getName() {
            return this.name;
        }

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

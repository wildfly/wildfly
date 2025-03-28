/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jboss.staxmapper.XMLMapper;
import org.wildfly.common.function.Functions;

/**
 * A tester for an {@link XMLElement}.
 */
public interface XMLElementTester<RC, WC> extends AutoCloseable {
    /**
     * Reads the specified string of XML into a reader context
     * @param xml an XML string
     * @return a reader context
     * @throws XMLStreamException if the specified XML could not be parsed
     */
    RC readElement(String xml) throws XMLStreamException;

    /**
     * Reads the specified context into XML
     * @param value a value to be written
     * @return an XML document for this element
     * @throws XMLStreamException if the specified value could not be written to XML
     */
    String writeElement(WC value) throws XMLStreamException;

    @Override
    void close();

    static XMLElementTester<Void, Void> of(XMLElement<Void, Void> element) {
        return of(element, Functions.<Void>constantSupplier(null));
    }

    static <RC, WC> XMLElementTester<RC, WC> of(XMLElement<RC, WC> element, Supplier<RC> contextFactory) {
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(element.getName(), element.getReader());
        return new XMLElementTester<>() {
            @Override
            public RC readElement(String xml) throws XMLStreamException {
                XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(new StringReader(xml)));
                try {
                    RC context = contextFactory.get();
                    mapper.parseDocument(context, reader);
                    return context;
                } finally {
                    reader.close();
                }
            }

            @Override
            public String writeElement(WC value) throws XMLStreamException {
                StringWriter stringWriter = new StringWriter();
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new StreamResult(stringWriter));
                try {
                    writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
                    mapper.deparseDocument(element.getWriter(), value, writer);
                    writer.writeEndDocument();
                    return stringWriter.toString();
                } finally {
                    writer.close();
                }
            }

            @Override
            public void close() {
                mapper.unregisterRootElement(element.getName());
            }
        };
    }
}

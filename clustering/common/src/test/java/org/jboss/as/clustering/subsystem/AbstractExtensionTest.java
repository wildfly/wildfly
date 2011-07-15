/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;

/**
 * Performs a read-write-read cycle of a subsystem's configuration
 * @author Paul Ferraro
 */
public abstract class AbstractExtensionTest {
    private final XMLElementReader<List<ModelNode>> reader;
    private final XMLElementWriter<SubsystemMarshallingContext> writer;
    private final String path;
    private final String namespace;
    
    protected AbstractExtensionTest(XMLElementReader<List<ModelNode>> reader, XMLElementWriter<SubsystemMarshallingContext> writer, String path, String namespace) {
        this.reader = reader;
        this.writer = writer;
        this.path = path;
        this.namespace = namespace;
    }
    
    @Test
    public void test() throws Exception {

        URL url = Thread.currentThread().getContextClassLoader().getResource(this.path);
        assertNotNull(url);

        List<ModelNode> operations = parse(new StreamSource(url.toString()));

        ModelNode model = this.populate(operations);

        String xml = this.write(model);

        assertEquals(operations, this.parse(new StreamSource(new StringReader(xml))));
    }

    protected abstract ModelNode populate(List<ModelNode> operations) throws OperationFailedException;
    
    private List<ModelNode> parse(Source source) throws XMLStreamException, IOException {
        final List<ModelNode> operations = new ArrayList<ModelNode>();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(this.namespace, "subsystem"), this.reader);

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(source);
        mapper.parseDocument(operations, reader);

        return operations;
    }
    
    private String write(ModelNode model) throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newFactory().createXMLStreamWriter(result));
        this.writer.writeContent(writer, new SubsystemMarshallingContext(model, writer));
        writer.flush();
        return result.toString();
    }
}

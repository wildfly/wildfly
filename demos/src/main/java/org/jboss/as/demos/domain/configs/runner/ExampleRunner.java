/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.domain.configs.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.domain.client.api.DomainClient;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * Demonstration of basic aspects of reading domain and host controller configurations
 * via the domain management API.
 *
 * @author Brian Stansberry
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DomainClient client = null;
        try {
            client = DomainClient.Factory.create(InetAddress.getByName("localhost"), 9999);

            System.out.println("\nReading the domain configuration:\n");
            System.out.println(writeModel("domain", client.getDomainModel()));
            System.out.println("\nReading the list of active host controllers:\n");
            List<String> hostControllers = client.getHostControllerNames();
            for (String hc : hostControllers) {
                System.out.println(hc);
            }

            for (String hc : hostControllers) {
                System.out.println("\nReading host configuration for host controller " + hc + "\n");
                System.out.println(writeModel("host", client.getHostModel(hc)));
            }

        } finally {
            safeClose(client);
        }
    }

    private static String writeModel(final String element, final XMLContentWriter content) throws Exception, FactoryConfigurationError {
        final XMLMapper mapper = XMLMapper.Factory.create();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BufferedOutputStream bos = new BufferedOutputStream(baos);
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(bos);
        try {
            mapper.deparseDocument(new RootElementWriter(element, content), writer);
        }
        catch (XMLStreamException e) {
            // Dump some diagnostics
            System.out.println("XML Content that was written prior to exception:");
            System.out.println(writer.toString());
            throw e;
        }
        finally {
            writer.close();
            bos.close();
        }
        return new String(baos.toByteArray());
    }

    private static class RootElementWriter implements XMLContentWriter {

        private final String element;
        private final XMLContentWriter content;

        RootElementWriter(final String element, final XMLContentWriter content) {
            this.element = element;
            this.content = content;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement(element);
            content.writeContent(streamWriter);
            streamWriter.writeEndDocument();
        }

    }

}

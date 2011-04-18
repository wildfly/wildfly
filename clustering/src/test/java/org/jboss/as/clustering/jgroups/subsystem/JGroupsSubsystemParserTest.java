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
package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.jboss.as.clustering.jgroups.subsystem.Namespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;

public class JGroupsSubsystemParserTest {

    private JGroupsExtension parser = new JGroupsExtension();

    @Test
    public void parse() throws Exception {

        List<ModelNode> operations = parse("subsystem-jgroups.xml");
        System.out.println(operations);
        Assert.assertEquals(3, operations.size());
//        final ModelNode operation = operations.get(0);
//        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newFactory().createXMLStreamWriter(System.out));
//        this.parser.writeContent(writer, new SubsystemMarshallingContext(operation, writer));
//        writer.flush();
    }

    private List<ModelNode> parse(String name) throws XMLStreamException, IOException {
        final List<ModelNode> operations = new ArrayList<ModelNode>();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.CURRENT.getUri(), "subsystem"), this.parser);

        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        Assert.assertNotNull(url);

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(url.toString()));
        mapper.parseDocument(operations, reader);

        return operations;
    }
}

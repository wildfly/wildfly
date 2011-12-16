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
package org.jboss.as.test.smoke.embedded.mgmt.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.NewDataSourceSubsystemParser;

import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.staxmapper.XMLMapper;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import java.util.ArrayList;
import org.junit.Assert;
import java.util.List;
import org.jboss.dmr.Property;
import java.util.HashMap;

public class DataSourceOperationTestUtil {


    static void testConnection(final String dsName, final ModelControllerClient client) throws Exception {
        final ModelNode address3 = new ModelNode();
        address3.add("subsystem", "datasources");
        address3.add("data-source", dsName);
        address3.protect();

        final ModelNode operation3 = new ModelNode();
        operation3.get(OP).set("test-connection-in-pool");
        operation3.get(OP_ADDR).set(address3);

        final ModelNode result3 = client.execute(operation3);
        Assert.assertEquals(SUCCESS, result3.get(OUTCOME).asString());
    }

    static void testConnectionXA(final String dsName, final ModelControllerClient client) throws Exception {
        final ModelNode address3 = new ModelNode();
        address3.add("subsystem", "datasources");
        address3.add("xa-data-source", dsName);
        address3.protect();

        final ModelNode operation3 = new ModelNode();
        operation3.get(OP).set("test-connection-in-pool");
        operation3.get(OP_ADDR).set(address3);

        final ModelNode result3 = client.execute(operation3);
        Assert.assertEquals(SUCCESS, result3.get(OUTCOME).asString());
    }
    static List<ModelNode> marshalAndReparseDsResources(final String childType,ModelControllerClient client) throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set(childType);
        operation.get(RECURSIVE).set(true);
        operation.get(OP_ADDR).set(address);

        final ModelNode ret = client.execute(operation);
        Assert.assertTrue("Management operation " + operation.asString() + " failed: " + ret.asString(),
                SUCCESS.equals(ret.get(OUTCOME).asString()));
        final ModelNode result = ret.get(RESULT);
        Assert.assertNotNull(result);
        
        final Map<String, ModelNode> children = getChildren(result);
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assert.assertTrue(child.getKey() != null);
            Assert.assertTrue(child.getValue().hasDefined("jndi-name"));
            Assert.assertTrue(child.getValue().hasDefined("driver-name"));
        }

        ModelNode dsNode = new ModelNode();
        dsNode.get(childType).set(result);

        StringWriter strWriter = new StringWriter();
        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newFactory()
                .createXMLStreamWriter(strWriter));
        NewDataSourceSubsystemParser parser = new NewDataSourceSubsystemParser();
        parser.writeContent(writer, new SubsystemMarshallingContext(dsNode, writer));
        writer.flush();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), "subsystem"), parser);

        StringReader strReader = new StringReader(strWriter.toString());

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(strReader));
        List<ModelNode> newList = new ArrayList<ModelNode>();
        mapper.parseDocument(newList, reader);
        return newList;
    }
    private static Map<String, ModelNode> getChildren(final ModelNode result) {
        Assert.assertTrue(result.isDefined());
        final Map<String, ModelNode> steps = new HashMap<String, ModelNode>();
        for (final Property property : result.asPropertyList()) {
            steps.put(property.getName(), property.getValue());
        }
        return steps;
    }
}

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

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.getChildren;


import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.NewDataSourceSubsystemParser;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Datasource operation unit test.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
@RunWith(Arquillian.class)
@RunAsClient

public class AddMySqlDataSourceOperationsUnitTestCase {

    private ModelControllerClient client;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
				File jdbcJar = new File( System.getProperty("jbossas.ts.integ.dir", "."),
                                 "/smoke/src/test/resources/mysql-connector-java-5.1.15.jar");
        if( ! jdbcJar.exists() )
            throw new IllegalStateException("Can't find " + jdbcJar + " using ${jbossas.ts.integ.dir} == " + System.getProperty("jbossas.ts.integ.dir") );
        Archive<?> archive = ShrinkWrap.createFromZipFile(JavaArchive.class, jdbcJar);
        Node node = archive.get("META-INF");
        return archive;
        // ShrinkWrapUtils.createJavaArchive("mysql-connector-java-5.1.15.jar").getResources("mysql-connector-java-5.1.15.jar");
    }

    // [ARQ-458] @Before not called with @RunAsClient
    private ModelControllerClient getModelControllerClient() throws UnknownHostException {
        StreamUtils.safeClose(client);
        client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
        return client;
    }

    @After
    public void tearDown() {
        StreamUtils.safeClose(client);
    }

    @Test
    public void testAddDsAndTestConnection() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", "MySqlDs");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set("MySqlDs");
        operation.get("jndi-name").set("java:jboss/datasources/MySqlDs");
        operation.get("enabled").set(true);

        operation.get("driver-name").set("mysql-connector-java-5.1.15.jar");
        operation.get("pool-name").set("MySqlDs_Pool");

        operation.get("connection-url").set("dont_care");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode result = getModelControllerClient().execute(operation);

        List<ModelNode> newList = marshalAndReparseDsResources();

        Assert.assertNotNull(newList);

        boolean containsRightJndiname = false;
        for(ModelNode res : newList){
            final Map<String, ModelNode> parseChildren = getChildren(res);
            if (! parseChildren.isEmpty() && parseChildren.get("jndi-name")!= null && parseChildren.get("jndi-name").asString().equals("java:jboss/datasources/MySqlDs")) {
                containsRightJndiname = true;
            }
        }
        
        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        getModelControllerClient().execute(compensatingOperation);
        Assert.assertTrue(containsRightJndiname);
    }

    public List<ModelNode> marshalAndReparseDsResources() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set("data-source");
        operation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final Map<String, ModelNode> children = DataSourceOperationTestUtil.getChildren(result.get(RESULT));
        Assert.assertFalse(children.isEmpty());
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assert.assertTrue(child.getKey() != null);
            Assert.assertTrue(child.getValue().hasDefined("connection-url"));
            Assert.assertTrue(child.getValue().hasDefined("jndi-name"));
            Assert.assertTrue(child.getValue().hasDefined("driver-name"));
        }

        ModelNode dsNode = new ModelNode();
        dsNode.get("data-source").set(result.get("result"));

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

}

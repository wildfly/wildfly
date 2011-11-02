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

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.NewDataSourceSubsystemParser;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.getChildren;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.testConnection;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.testConnectionXA;

/**
 * Datasource operation unit test.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceOperationsUnitTestCase {

    private ModelControllerClient client;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrapUtils.createEmptyJavaArchive("dummy");
    }

    // [ARQ-458] @Before not called with @RunAsClient
    private ModelControllerClient getModelControllerClient() throws UnknownHostException {
        if (client == null) {
            client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        }
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
        address.add("data-source", "MyNewDs");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set("MyNewDs");
        operation.get("jndi-name").set("java:jboss/datasources/MyNewDs");


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set("MyNewDs_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());


        testConnection("MyNewDs", getModelControllerClient());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals("java:jboss/datasources/MyNewDs", parseChildren.get("jndi-name").asString());

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        getModelControllerClient().execute(compensatingOperation);
    }

    /**
     * AS7-1202 test for enable datasource
     *
     * @throws Exception
     */
    @Test
    public void testAddDisabledDsEnableItAndTestConnection() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", "MyNewDs");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set("MyNewDs");
        operation.get("jndi-name").set("java:jboss/datasources/MyNewDs");

        operation.get("driver-name").set("h2");
        operation.get("pool-name").set("MyNewDs_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());

        testConnection("MyNewDs", getModelControllerClient());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals("java:jboss/datasources/MyNewDs", parseChildren.get("jndi-name").asString());

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        getModelControllerClient().execute(compensatingOperation);
    }

    @Test
    public void testAddDsWithConnectionProperties() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", "MyNewDs");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set("MyNewDs");
        operation.get("jndi-name").set("java:jboss/datasources/MyNewDs");


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set("MyNewDs_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());


        final ModelNode connectionPropertyAddress = address.clone();
        connectionPropertyAddress.add("connection-properties", "MyKey");
        connectionPropertyAddress.protect();

        final ModelNode connectionPropertyOperation = new ModelNode();
        connectionPropertyOperation.get(OP).set("add");
        connectionPropertyOperation.get(OP_ADDR).set(connectionPropertyAddress);


        connectionPropertyOperation.get("value").set("MyValue");


        final ModelNode connectionPropertyResult = getModelControllerClient().execute(connectionPropertyOperation);
        Assert.assertEquals(SUCCESS, connectionPropertyResult.get(OUTCOME).asString());

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals("java:jboss/datasources/MyNewDs", parseChildren.get("jndi-name").asString());

        //TODO: verify connection-properties
        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        getModelControllerClient().execute(compensatingOperation);
    }

    @Test
    public void testAddAndRemoveSameName() throws Exception {
        final String dsName = "SameNameDs";
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", dsName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(dsName);
        operation.get("jndi-name").set("java:jboss/datasources/" + dsName);

        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);
        // do twice, test for AS7-720
        for (int i = 1; i <= 2; i++) {
            final ModelNode result = getModelControllerClient().execute(operation);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

            getModelControllerClient().execute(compensatingOperation);
        }
    }

    /**
     * AS7-1206 test for jndi binding isn't unbound during remove if jndi name
     * and data-source name are different
     *
     * @throws Exception
     */
    //@Test
    public void testAddAndRemoveNameAndJndiNameDifferent() throws Exception {
        final String dsName = "DsName";
        final String jndiDsName = "JndiDsName";

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", dsName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(dsName);
        operation.get("jndi-name").set("java:jboss/datasources/" + jndiDsName);


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);
        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final ModelNode compensatinResult = getModelControllerClient().execute(compensatingOperation);
        Assert.assertEquals(SUCCESS, compensatinResult.get(OUTCOME).asString());

    }

    @Test
    public void testAddAndRemoveXaDs() throws Exception {
        final String dsName = "XaDsName";
        final String jndiDsName = "XaJndiDsName";

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", dsName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(dsName);
        operation.get("jndi-name").set("java:jboss/datasources/" + jndiDsName);


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        final ModelNode connectionPropertyResult = getModelControllerClient().execute(xaDatasourcePropertyOperation);
        Assert.assertEquals(SUCCESS, connectionPropertyResult.get(OUTCOME).asString());


        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());


        testConnectionXA(dsName, getModelControllerClient());

        final ModelNode propertyCompensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        getModelControllerClient().execute(propertyCompensatingOperation);

        final ModelNode compensatinResult = getModelControllerClient().execute(compensatingOperation);
        Assert.assertEquals(SUCCESS, compensatinResult.get(OUTCOME).asString());

    }

    /**
     * AS7-1200 test case for xa datasource persistence to xml
     *
     * @throws Exception
     */
    @Test
    public void testMarshallUnmarshallXaDs() throws Exception {
        final String dsName = "XaDsName2";
        final String jndiDsName = "XaJndiDsName";

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", dsName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(dsName);
        operation.get("jndi-name").set("java:jboss/datasources/" + jndiDsName);


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        final ModelNode connectionPropertyResult = getModelControllerClient().execute(xaDatasourcePropertyOperation);
        Assert.assertEquals(SUCCESS, connectionPropertyResult.get(OUTCOME).asString());

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals("java:jboss/datasources/XaJndiDsName", parseChildren.get("jndi-name").asString());

        final ModelNode propertyCompensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        getModelControllerClient().execute(propertyCompensatingOperation);

        final ModelNode compensatinResult = getModelControllerClient().execute(compensatingOperation);
        Assert.assertEquals(SUCCESS, compensatinResult.get(OUTCOME).asString());

        // remove from xml too
        marshalAndReparseDsResources("xa-data-source");

    }

    /**
     * AS7-1201 test for en/diable xa datasources
     *
     * @throws Exception
     */
    @Test
    public void DisableAndReEnableXaDs() throws Exception {
        final String dsName = "XaDsNameDisEn";
        final String jndiDsName = "XaJndiDsNameDisEn";

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", dsName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(dsName);
        operation.get("jndi-name").set("java:jboss/datasources/" + jndiDsName);


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        final ModelNode enableOperation = new ModelNode();
        enableOperation.get(OP).set("enable");
        enableOperation.get(OP_ADDR).set(address);

        final ModelNode disableOperation = new ModelNode();
        disableOperation.get(OP).set("disable");
        disableOperation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        final ModelNode connectionPropertyResult = getModelControllerClient().execute(xaDatasourcePropertyOperation);
        Assert.assertEquals(SUCCESS, connectionPropertyResult.get(OUTCOME).asString());

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());

        testConnectionXA(dsName, getModelControllerClient());

        getModelControllerClient().execute(disableOperation);
        getModelControllerClient().execute(enableOperation);

        testConnectionXA(dsName, getModelControllerClient());

        final ModelNode propertyCompensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        getModelControllerClient().execute(propertyCompensatingOperation);

        final ModelNode compensatinResult = getModelControllerClient().execute(compensatingOperation);
        Assert.assertEquals(SUCCESS, compensatinResult.get(OUTCOME).asString());

    }

    @Test
    public void testReadInstalledDrivers() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("installed-drivers-list");
        operation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode result2 = result.get(RESULT).get(0);
        Assert.assertTrue(result2 != null);
        Assert.assertTrue(result2.hasDefined("driver-module-name"));
        Assert.assertTrue(result2.hasDefined("module-slot"));
        Assert.assertTrue(result2.hasDefined("driver-name"));
    }

    /**
     * AS7-1203 test for missing xa-datasource properties
     *
     * @throws Exception
     */
    @Test
    public void testAddXaDsWithProperties() throws Exception {

        final String xaDs = "MyNewXaDs";
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", xaDs);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(xaDs);
        operation.get("jndi-name").set("java:jboss/xa-datasources/" + xaDs);
        operation.get("driver-name").set("h2");

        operation.get("pool-name").set(xaDs + "_Pool");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        final ModelNode connectionPropertyResult = getModelControllerClient().execute(xaDatasourcePropertyOperation);
        Assert.assertEquals(SUCCESS, connectionPropertyResult.get(OUTCOME).asString());


        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        final ModelNode result2 = getModelControllerClient().execute(operation2);
        Assert.assertEquals(SUCCESS, result2.get(OUTCOME).asString());



        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals("java:jboss/xa-datasources/" + xaDs, parseChildren.get("jndi-name").asString());

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(address);

        getModelControllerClient().execute(compensatingOperation);
    }

    public List<ModelNode> marshalAndReparseDsResources(final String childType) throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set(childType);
        operation.get(RECURSIVE).set(true);
        operation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final Map<String, ModelNode> children = getChildren(result.get(RESULT));
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assert.assertTrue(child.getKey() != null);
            // Assert.assertTrue(child.getValue().hasDefined("connection-url"));
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

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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.TunneledMBeanServerConnection;
import org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.smoke.embedded.demos.fakejndi.FakeJndi;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.addExtensionProperties;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.checkModelParams;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.nonXaDsProperties;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.setOperationParams;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.xaDsProperties;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.marshalAndReparseDsResources;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.testConnection;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.testConnectionXA;


/**
 * Datasource operation unit test.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceOperationsUnitTestCase extends AbstractMgmtTestBase{

    @Deployment
    public static Archive<?> getDeployment() {
    	initModelControllerClient("localhost",9999);
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        return ShrinkWrapUtils.createJavaArchive("demos/fakejndi.sar", FakeJndi.class.getPackage());
    }

    @AfterClass
    public static void tearDown() throws IOException{
    	closeModelControllerClient();
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

        executeOperation(operation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);

        testConnection("MyNewDs", getModelControllerClient());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        Assert.assertNotNull(findNodeWithProperty(newList,"jndi-name","java:jboss/datasources/MyNewDs"));
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

        executeOperation(operation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);

        testConnection("MyNewDs", getModelControllerClient());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        Assert.assertNotNull(findNodeWithProperty(newList,"jndi-name","java:jboss/datasources/MyNewDs"));
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

        executeOperation(operation);


        final ModelNode connectionPropertyAddress = address.clone();
        connectionPropertyAddress.add("connection-properties", "MyKey");
        connectionPropertyAddress.protect();

        final ModelNode connectionPropertyOperation = new ModelNode();
        connectionPropertyOperation.get(OP).set("add");
        connectionPropertyOperation.get(OP_ADDR).set(connectionPropertyAddress);


        connectionPropertyOperation.get("value").set("MyValue");


        executeOperation(connectionPropertyOperation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);

        List<ModelNode> newList = marshalAndReparseDsResources("data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        Assert.assertNotNull(findNodeWithProperty(newList,"jndi-name","java:jboss/datasources/MyNewDs"));
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

        // do twice, test for AS7-720
        for (int i = 1; i <= 2; i++) {
            executeOperation(operation);

            remove(address);
        }
    }

    /**
     * AS7-1206 test for jndi binding isn't unbound during remove if jndi name
     * and data-source name are different
     *
     * @throws Exception
     */
    @Test
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


        executeOperation(operation);
        remove(address);

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


        executeOperation(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        executeOperation(xaDatasourcePropertyOperation);


        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);


        testConnectionXA(dsName, getModelControllerClient());

        remove(address);
    }

    /**
     * AS7-1200 test case for xa datasource persistence to xml
     *
     * @throws Exception
     */
    @Test
    public void testMarshallUnmarshallXaDs() throws Exception {
        final String dsName = "XaDsName2";
        final String jndiDsName = "XaJndiDsName2";

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

        executeOperation(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        executeOperation(xaDatasourcePropertyOperation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        // remove from xml too
        marshalAndReparseDsResources("xa-data-source",getModelControllerClient());

        Assert.assertNotNull(findNodeWithProperty(newList,"jndi-name","java:jboss/datasources/" + jndiDsName));

    }

    /**
     * AS7-1201 test for en/diable xa datasources
     *
     * @throws Exception
     */
    @Test
    @Ignore("AS7-3173")
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

        final ModelNode enableOperation = new ModelNode();
        enableOperation.get(OP).set("enable");
        enableOperation.get(OP_ADDR).set(address);

        final ModelNode disableOperation = new ModelNode();
        disableOperation.get(OP).set("disable");
        disableOperation.get(OP_ADDR).set(address);

        executeOperation(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        executeOperation(xaDatasourcePropertyOperation);

        executeOperation(enableOperation);

        testConnectionXA(dsName, getModelControllerClient());

        executeOperation(disableOperation);
        executeOperation(enableOperation);

        testConnectionXA(dsName, getModelControllerClient());

        remove(address);
    }

    @Test
    public void testReadInstalledDrivers() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("installed-drivers-list");
        operation.get(OP_ADDR).set(address);

        final ModelNode result = executeOperation(operation);

        final ModelNode result2 = result.get(0);
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
        final String xaDsJndi = "java:jboss/xa-datasources/" + xaDs;
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", xaDs);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("name").set(xaDs);
        operation.get("jndi-name").set(xaDsJndi);
        operation.get("driver-name").set("h2");
        operation.get("xa-datasource-class").set("org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource");
        operation.get("pool-name").set(xaDs + "_Pool");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        executeOperation(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        executeOperation(xaDatasourcePropertyOperation);


        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);



        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        ModifiableXaDataSource jxaDS = null;

        try{
            jxaDS = lookup(getModelControllerClient(), xaDsJndi ,ModifiableXaDataSource .class);

            Assert.fail("found datasource after it was unbounded");
        }
        catch (Exception e){
            // must be thrown NameNotFound exception - datasource is unbounded

        }

        Assert.assertNotNull(findNodeWithProperty(newList,"jndi-name",xaDsJndi));

    }
    /**
     * AS7-2720 tests for parsing particular datasource in standalone mode
     *
     * @throws Exception
     */
    @Test
    public void testAddComplexDs() throws Exception {

        final String complexDs = "complexDs";
        final String complexDsJndi = "java:jboss/datasources/" + complexDs;
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", complexDs);
        address.protect();

        Properties params=nonXaDsProperties(complexDsJndi);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        setOperationParams(operation, params);
        addExtensionProperties(operation);

        executeOperation(operation);

        final ModelNode datasourcePropertiesAddress = address.clone();
        datasourcePropertiesAddress.add("connection-properties", "char.encoding");
        datasourcePropertiesAddress.protect();
        final ModelNode datasourcePropertyOperation = new ModelNode();
        datasourcePropertyOperation.get(OP).set("add");
        datasourcePropertyOperation.get(OP_ADDR).set(datasourcePropertiesAddress);
        datasourcePropertyOperation.get("value").set("UTF-8");

        executeOperation(datasourcePropertyOperation);

        List<ModelNode> newList = marshalAndReparseDsResources("data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        ModelNode rightChild=findNodeWithProperty(newList,"jndi-name",complexDsJndi);
        
        Assert.assertTrue("node:"+rightChild.asString()+";\nparams"+params,checkModelParams(rightChild, params));

    }
    /**
     * AS7-2720 tests for parsing particular XA-datasource in standalone mode
     *
     * @throws Exception
     */
    @Test
    public void testAddComplexXaDs() throws Exception {

        final String complexXaDs = "complexXaDs";
        final String complexXaDsJndi = "java:jboss/xa-datasources/" + complexXaDs;

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", complexXaDs);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        Properties params=xaDsProperties(complexXaDsJndi);
        setOperationParams(operation, params);
        addExtensionProperties(operation);

        /* TODO: Properties for Extension type parameters not implemented in DRM
         * operation.get("recovery-plugin-properties","Property").set("A");
         */
        executeOperation(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        executeOperation(xaDatasourcePropertyOperation);

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source",getModelControllerClient());

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        ModelNode rightChild=findNodeWithProperty(newList,"jndi-name",complexXaDsJndi);
        
        Assert.assertTrue("node:"+rightChild.asString()+";\nparams"+params,checkModelParams(rightChild, params));

    }


    private static <T> T lookup(ModelControllerClient client, String name, Class<T> expected) throws Exception {
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working

        MBeanServerConnection mbeanServer = new TunneledMBeanServerConnection(client);
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, objectName));
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
    }


}

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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.getChildren;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.testConnection;
import static org.jboss.as.test.smoke.embedded.mgmt.datasource.DataSourceOperationTestUtil.testConnectionXA;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.TunneledMBeanServerConnection;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.NewDataSourceSubsystemParser;
import org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.smoke.embedded.demos.fakejndi.FakeJndi;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;


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

    private ModelNode execute(final ModelNode operation) throws IOException {
        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(operation + "\n" + result, SUCCESS, result.get(OUTCOME).asString());
        return result;
    }

    @Deployment
    public static Archive<?> getDeployment() {
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        return ShrinkWrapUtils.createJavaArchive("demos/fakejndi.sar", FakeJndi.class.getPackage());
    }

    // [ARQ-458] @Before not called with @RunAsClient
    private ModelControllerClient getModelControllerClient() throws UnknownHostException {
        if (client == null) {
            client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
        }
        return client;
    }

    private void remove(final ModelNode address) throws IOException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        execute(operation);
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

        execute(operation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        execute(operation2);

        testConnection("MyNewDs", getModelControllerClient());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);

        boolean containsRightJndiname = false;
        for(ModelNode result : newList){
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (! parseChildren.isEmpty() && parseChildren.get("jndi-name")!= null && parseChildren.get("jndi-name").asString().equals("java:jboss/datasources/MyNewDs")) {
                containsRightJndiname = true;
            }
        }
        
        remove(address);
        Assert.assertTrue(containsRightJndiname);
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

        execute(operation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        execute(operation2);

        testConnection("MyNewDs", getModelControllerClient());

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);

        boolean containsRightJndiname = false;
        for (ModelNode result : newList) {
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (!parseChildren.isEmpty() && parseChildren.get("jndi-name") != null && parseChildren.get("jndi-name").asString().equals("java:jboss/datasources/MyNewDs")) {
                containsRightJndiname = true;
            }
        }
        remove(address);
        Assert.assertTrue(containsRightJndiname);
        
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

        execute(operation);


        final ModelNode connectionPropertyAddress = address.clone();
        connectionPropertyAddress.add("connection-properties", "MyKey");
        connectionPropertyAddress.protect();

        final ModelNode connectionPropertyOperation = new ModelNode();
        connectionPropertyOperation.get(OP).set("add");
        connectionPropertyOperation.get(OP_ADDR).set(connectionPropertyAddress);


        connectionPropertyOperation.get("value").set("MyValue");


        execute(connectionPropertyOperation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        execute(operation2);

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);

       boolean containsRightJndiname = false;
        for(ModelNode result : newList){
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (! parseChildren.isEmpty() && parseChildren.get("jndi-name")!= null && parseChildren.get("jndi-name").asString().equals("java:jboss/datasources/MyNewDs")) {
                containsRightJndiname = true;
            }
        }
        remove(address);
        Assert.assertTrue(containsRightJndiname);
        
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
            execute(operation);

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


        execute(operation);
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


        execute(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        execute(xaDatasourcePropertyOperation);


        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        execute(operation2);


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

        execute(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        execute(xaDatasourcePropertyOperation);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        execute(operation2);

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals("java:jboss/datasources/XaJndiDsName2", parseChildren.get("jndi-name").asString());

        remove(address);
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

        final ModelNode enableOperation = new ModelNode();
        enableOperation.get(OP).set("enable");
        enableOperation.get(OP_ADDR).set(address);

        final ModelNode disableOperation = new ModelNode();
        disableOperation.get(OP).set("disable");
        disableOperation.get(OP_ADDR).set(address);

        execute(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        execute(xaDatasourcePropertyOperation);

        execute(enableOperation);

        testConnectionXA(dsName, getModelControllerClient());

        execute(disableOperation);
        execute(enableOperation);

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

        final ModelNode result = execute(operation);

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

        execute(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        execute(xaDatasourcePropertyOperation);


        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("enable");
        operation2.get(OP_ADDR).set(address);

        execute(operation2);



        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        Assert.assertNotNull(newList);

        final Map<String, ModelNode> parseChildren = getChildren(newList.get(1));
        Assert.assertFalse(parseChildren.isEmpty());
        Assert.assertEquals(xaDsJndi, parseChildren.get("jndi-name").asString());

        remove(address);

        ModifiableXaDataSource jxaDS = null;
        try{
            jxaDS = lookup(client, xaDsJndi ,ModifiableXaDataSource .class);

            Assert.fail("found datasource after it was unbounded");
        }
        catch (Exception e){
            // must be thrown NameNotFound exception - datasource is unbounded

        }
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

        Hashtable<String,String> params=hashtableWithNonXaParameters(complexDs,complexDsJndi);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        setOperationParams(operation, params);
        addExtensionProperties(operation);

        execute(operation);

        final ModelNode datasourcePropertiesAddress = address.clone();
        datasourcePropertiesAddress.add("connection-properties", "char.encoding");
        datasourcePropertiesAddress.protect();
        final ModelNode datasourcePropertyOperation = new ModelNode();
        datasourcePropertyOperation.get(OP).set("add");
        datasourcePropertyOperation.get(OP_ADDR).set(datasourcePropertiesAddress);
        datasourcePropertyOperation.get("value").set("UTF-8");

        execute(datasourcePropertyOperation);

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        Assert.assertNotNull(newList);


        Map<String, ModelNode> rightChildren = Collections.emptyMap();
        for(ModelNode result : newList){
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (! parseChildren.isEmpty() && parseChildren.get("jndi-name")!= null && parseChildren.get("jndi-name").asString().equals("java:jboss/datasources/complexDs")) {
                rightChildren = parseChildren;
            }
        }
        Assert.assertFalse(rightChildren.isEmpty());

        controlParseChildrenParams(rightChildren, params);

        remove(address);

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

        Hashtable<String,String> params = hashtableWithXaParameters(complexXaDs, complexXaDsJndi) ;
        setOperationParams(operation, params);
        addExtensionProperties(operation);

        /* TODO: Properties for Extension type parameters not implemented in DRM
         * operation.get("recovery-plugin-properties","Property").set("A");
         */
        execute(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        execute(xaDatasourcePropertyOperation);

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        Assert.assertNotNull(newList);

        Map<String, ModelNode> parseChildren = null;
        boolean containsRightJndiname = false;
        for (ModelNode result : newList) {
            parseChildren = getChildren(result);
            if (!parseChildren.isEmpty() && parseChildren.get("jndi-name") != null && parseChildren.get("jndi-name").asString().equals(complexXaDsJndi)) {
                containsRightJndiname = true;
                break;
            }
        }

        remove(address);

        Assert.assertTrue(containsRightJndiname);
        controlParseChildrenParams(parseChildren, params);
    }
    /**
     * Returns Hashtable with common parameters for both XA and Non-XA datasource
     *
     */
    private  Hashtable<String,String> hashtableWithCommonParameters(){
    	Hashtable<String,String> params=new Hashtable<String,String>();
    	//attributes
    	params.put("use-java-context","true");
        params.put("spy","false");
        params.put("use-ccm","true");
        //common elements
        params.put("driver-name","h2");
        params.put("new-connection-sql","select 1");
        params.put("transaction-isolation","TRANSACTION_READ_COMMITTED");
        params.put("url-delimiter",":");
        params.put("url-selector-strategy-class-name","someClass");
        //pool
        params.put("min-pool-size","1");
        params.put("max-pool-size","5");
        params.put("pool-prefill","true");
        params.put("pool-use-strict-min","true");
        params.put("flush-strategy","EntirePool");
        //security
        params.put("user-name","sa");
        params.put("password","sa");
        params.put("security-domain","HsqlDbRealm");
        params.put("reauth-plugin-class-name","someClass1");
        //validation
        params.put("valid-connection-checker-class-name","someClass2");
        params.put("check-valid-connection-sql","select 1");
        params.put("validate-on-match","true");
        params.put("background-validation","true");
        params.put("background-validation-millis","2000");
        params.put("use-fast-fail","true");
        params.put("stale-connection-checker-class-name","someClass3");
        params.put("exception-sorter-class-name","someClass4");
        //time-out
        params.put("blocking-timeout-wait-millis","20000");
        params.put("idle-timeout-minutes","4");
        params.put("set-tx-query-timeout","true");
        params.put("query-timeout","120");
        params.put("use-try-lock","100");
        params.put("allocation-retry","2");
        params.put("allocation-retry-wait-millis","3000");
        //statement
        params.put("track-statements","NOWARN");
        params.put("prepared-statements-cache-size","30");
        params.put("share-prepared-statements","true");

    	return params;
    }
    /**
     * Returns a Hashtable, containing parameters for XA datasource
     * @param datasourceName
     */
    private Hashtable<String,String> hashtableWithXaParameters(String datasourceName,String jndiName){
    	Hashtable<String,String> params=hashtableWithCommonParameters();
    	//attributes
    	params.put("jndi-name", jndiName);
        //common
        params.put("xa-datasource-class","org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource");
        //xa-pool
        params.put("same-rm-override","true");
        params.put("interleaving","true");
        params.put("no-tx-separate-pool","true");
        params.put("pad-xid","true");
        params.put("wrap-xa-resource","true");
        //time-out
        params.put("xa-resource-timeout","120");
        //recovery
        params.put("no-recovery","false");
        params.put("recovery-plugin-class-name","someClass5");
        params.put("recovery-username","sa");
        params.put("recovery-password","sa");
        params.put("recovery-security-domain","HsqlDbRealm");


    	return params;
    }
    /**
     * Returns a Hashtable, containing parameters for non XA datasource
     * @param datasourceName
     */
    private Hashtable<String,String> hashtableWithNonXaParameters(String datasourceName,String jndiName){
    	Hashtable<String,String> params=hashtableWithCommonParameters();
    	//attributes
        params.put("jndi-name",jndiName);
        params.put("jta","false");
        //common
        params.put("driver-class","org.hsqldb.jdbcDriver");
        params.put("datasource-class","org.jboss.as.connector.subsystems.datasources.ModifiableDataSource");
        params.put("connection-url","jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        return params;
    }
    /**
     * Sets parameters for DMR operation
     * @param operation
     * @param params
     */
    private void setOperationParams(ModelNode operation,Hashtable<String,String> params){
    	String str;
        Iterator it=params.keySet().iterator();
        while(it.hasNext()){
        	str=(String)it.next();
        	operation.get(str).set(params.get(str));
        }
    }
    /**
     * Adds properties of Extension type to the operation
     * TODO: not implemented jet in DRM
     */
    private void addExtensionProperties(ModelNode operation){
    	/*

        operation.get("reauth-plugin-properties","Property").set("A");
        operation.get("valid-connection-checker-properties","Property").set("B");
        operation.get("stale-connect,roperties","Property").set("C");
        operation.get("exception-sorter-properties","Property").set("D");
       */
        /*final ModelNode sourcePropertiesAddress = address.clone();
        sourcePropertiesAddress.add("reauth-plugin-properties", "Property");
        sourcePropertiesAddress.protect();
        final ModelNode sourcePropertyOperation = new ModelNode();
        sourcePropertyOperation.get(OP).set("add");
        sourcePropertyOperation.get(OP_ADDR).set(sourcePropertiesAddress);
        sourcePropertyOperation.get("value").set("A");

        execute(sourcePropertyOperation);*/

    }
    /**
     * Controls if result of reparsing contains certain parameters
     * @param parseChildren
     * @param params
     */
    private void controlParseChildrenParams(Map<String,ModelNode> parseChildren,Hashtable<String,String> params){
    	String str;
        Iterator it=params.keySet().iterator();

        StringBuffer sb = new StringBuffer();
        String par,child;
        while(it.hasNext()){
        	str=(String)it.next();
        	par=params.get(str);
        	if (!parseChildren.containsKey(str)) sb.append("Parameter <"+str+"> is not set, but must be set to '"+par+"' \n");
        	else{
        		child= parseChildren.get(str).asString();
        		if (!child.equals(par)) sb.append("Parameter <"+str+"> is set to '"+child+"', but must be set to '"+par+"' \n");
        	}
        }
        if (sb.length()>0) Assert.fail("There are parsing errors:\n"+sb.toString()+"Parsed configuration:\n"+parseChildren);
    }

    private static <T> T lookup(ModelControllerClient client, String name, Class<T> expected) throws Exception {
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working

        MBeanServerConnection mbeanServer = new TunneledMBeanServerConnection(client);
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, objectName));
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
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

        final ModelNode result = execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        final Map<String, ModelNode> children = getChildren(result.get(RESULT));
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assert.assertTrue(child.getKey() != null);
            // Assert.assertTrue(child.getValue().hasDefined("connection-url"));
            Assert.assertTrue(child.getValue().hasDefined("jndi-name"));
            Assert.assertTrue(child.getValue().hasDefined("driver-name"));
        }

        ModelNode dsNode = new ModelNode();
        dsNode.get(childType).set(result.get("result"));

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

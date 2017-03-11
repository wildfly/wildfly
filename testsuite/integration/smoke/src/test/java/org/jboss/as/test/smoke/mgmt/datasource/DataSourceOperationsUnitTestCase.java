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

package org.jboss.as.test.smoke.mgmt.datasource;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.addExtensionProperties;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.checkModelParams;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.nonXaDsProperties;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.setOperationParams;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.xaDsProperties;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.jca.ConnectionSecurityType;
import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Datasource operation unit test.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DataSourceOperationsUnitTestCase.ServerSetup.class)
public class DataSourceOperationsUnitTestCase extends DsMgmtTestBase {

    public static class ServerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode authContextAdd = Util.createAddOperation(PathAddress.pathAddress("subsystem", "elytron").append("authentication-context", "HsqlAuthCtxt"));
            ModelNode response = managementClient.getControllerClient().execute(authContextAdd);
            Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode authContextRemove = Util.createRemoveOperation(PathAddress.pathAddress("subsystem", "elytron").append("authentication-context", "HsqlAuthCtxt"));
            ModelNode response = managementClient.getControllerClient().execute(authContextRemove);
            Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());
        }
    }

    @Deployment
    public static Archive<?> fakeDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
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
        operation.get("enabled").set(true);


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set("MyNewDs_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        executeOperation(operation);

        testConnection("MyNewDs");

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        remove(address);

        Assert.assertNotNull("Reparsing failed:", newList);

        Assert.assertNotNull(findNodeWithProperty(newList, "jndi-name", "java:jboss/datasources/MyNewDs"));
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
        operation.get("enabled").set(false);

        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
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
        operation.get("enabled").set(false);


        operation.get("driver-name").set("h2");
        operation.get("pool-name").set(dsName + "_Pool");

        operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
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
        operation.get("enabled").set(false);


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
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        executeOperation(xaDatasourcePropertyOperation);

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
        operation.get("enabled").set(false);


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
        operation2.get(OP).set("write-attribute");
        operation2.get("name").set("enabled");
        operation2.get("value").set(true);
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        remove(address);

        Assert.assertNotNull("Reparsing failed:", newList);

        // remove from xml too
        marshalAndReparseDsResources("xa-data-source");

        Assert.assertNotNull(findNodeWithProperty(newList, "jndi-name", "java:jboss/datasources/" + jndiDsName));

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
        Assert.assertNotNull("There are no installed JDBC drivers", result2);
        Assert.assertTrue("Name of JDBC driver is udefined", result2.hasDefined("driver-name"));
        if (!result2.hasDefined("deployment-name")) {//deployed drivers haven't these attributes
            Assert.assertTrue("Module name of JDBC driver is udefined", result2.hasDefined("driver-module-name"));
            Assert.assertTrue("Module slot of JDBC driver is udefined", result2.hasDefined("module-slot"));
        }
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
        operation.get("enabled").set(false);
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
        operation2.get(OP).set("write-attribute");
        operation2.get("name").set("enabled");
        operation2.get("value").set(true);
        operation2.get(OP_ADDR).set(address);

        executeOperation(operation2);


        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        remove(address);

        Assert.assertNotNull("Reparsing failed:", newList);

        Assert.assertNotNull(findNodeWithProperty(newList, "jndi-name", xaDsJndi));

    }

    /**
     * AS7-2720 tests for parsing particular datasource in standalone mode
     *
     * @throws Exception
     */
    @Test
    public void testAddComplexDsUsername() throws Exception {
        testAddComplexDs(ConnectionSecurityType.USER_PASSWORD);
    }

    @Test
    public void testAddComplexDsElytron() throws Exception {
        testAddComplexDs(ConnectionSecurityType.ELYTRON);
    }

    @Test
    public void testAddComplexDsElytronAuthenticationContext() throws Exception {
        testAddComplexDs(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT);
    }

    @Test
    public void testAddComplexDsSecurityDomain() throws Exception {
        testAddComplexDs(ConnectionSecurityType.SECURITY_DOMAIN);
    }

    private void testAddComplexDs(ConnectionSecurityType connectionSecurityType) throws Exception {
        final String complexDs;
        switch(connectionSecurityType) {
            case ELYTRON:
                complexDs = "complexDsElytronWithOutAuthCtx";
                break;
            case ELYTRON_AUTHENTICATION_CONTEXT:
                complexDs = "complexDsElytronWithAuthCtx";
                break;
            case SECURITY_DOMAIN:
                complexDs = "complexDs";
                break;
            case USER_PASSWORD:
                complexDs = "complexDsWithUserName";
                break;
            default:
                throw new InvalidParameterException("Unsupported connection security type for Data Sources: " +
                        connectionSecurityType);
        }
        final String complexDsJndi = "java:jboss/datasources/" + complexDs;
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", complexDs);
        address.protect();

        Properties params = nonXaDsProperties(complexDsJndi, connectionSecurityType);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("enabled").set(false);

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

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        remove(address);

        Assert.assertNotNull("Reparsing failed:", newList);

        ModelNode rightChild = findNodeWithProperty(newList, "jndi-name", complexDsJndi);

        Assert.assertTrue("node:" + rightChild.asString() + ";\nparams" + params, checkModelParams(rightChild, params));

        Assert.assertEquals(rightChild.asString(), "Property2", rightChild.get("valid-connection-checker-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property4", rightChild.get("exception-sorter-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property3", rightChild.get("stale-connection-checker-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property1", rightChild.get("reauth-plugin-properties", "name").asString());

        Assert.assertNotNull("connection-properties not propagated ", findNodeWithProperty(newList, "value", "UTF-8"));

    }

    /**
     * AS7-2720 tests for parsing particular XA-datasource in standalone mode
     *
     * @throws Exception
     */
    @Test
    public void testAddComplexXaDsUsername() throws Exception {
        testAddComplexXaDs(ConnectionSecurityType.USER_PASSWORD);
    }

    @Test
    public void testAddComplexXaDsElytron() throws Exception {
        testAddComplexXaDs(ConnectionSecurityType.ELYTRON);
    }

    @Test
    public void testAddComplexXaDsElytronAuthenticationContext() throws Exception {
        testAddComplexXaDs(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT);
    }

    @Test
    public void testAddComplexXaDsComplexDs() throws Exception {
        testAddComplexXaDs(ConnectionSecurityType.SECURITY_DOMAIN);
    }

    private void testAddComplexXaDs(ConnectionSecurityType connectionSecurityType) throws Exception {
        final String complexXaDs;
        switch (connectionSecurityType) {
            case ELYTRON:
                complexXaDs = "complexXaDsWithElytron";
                break;
            case ELYTRON_AUTHENTICATION_CONTEXT:
                complexXaDs = "complexXaDsWithElytronCtxt";
                break;
            case SECURITY_DOMAIN:
                complexXaDs = "complexXaDs";
                break;
            case USER_PASSWORD:
                complexXaDs = "complexXaDsWithUserName";
                break;
            default:
                throw new InvalidParameterException("Unsupported connection security type in data sources: " +
                connectionSecurityType);
        }

        final String complexXaDsJndi = "java:jboss/xa-datasources/" + complexXaDs;

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", complexXaDs);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("enabled").set(false);

        Properties params = xaDsProperties(complexXaDsJndi, connectionSecurityType);
        setOperationParams(operation, params);
        addExtensionProperties(operation);
        operation.get("recovery-plugin-properties", "name").set("Property5");
        operation.get("recovery-plugin-properties", "name1").set("Property6");


        executeOperation(operation);

        final ModelNode xaDatasourcePropertiesAddress = address.clone();
        xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
        xaDatasourcePropertiesAddress.protect();
        final ModelNode xaDatasourcePropertyOperation = new ModelNode();
        xaDatasourcePropertyOperation.get(OP).set("add");
        xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
        xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

        executeOperation(xaDatasourcePropertyOperation);

        List<ModelNode> newList = marshalAndReparseDsResources("xa-data-source");

        remove(address);

        Assert.assertNotNull("Reparsing failed:", newList);

        ModelNode rightChild = findNodeWithProperty(newList, "jndi-name", complexXaDsJndi);

        Assert.assertTrue("node:" + rightChild.asString() + ";\nparams" + params, checkModelParams(rightChild, params));

        Assert.assertEquals(rightChild.asString(), "Property2", rightChild.get("valid-connection-checker-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property4", rightChild.get("exception-sorter-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property3", rightChild.get("stale-connection-checker-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property1", rightChild.get("reauth-plugin-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property5", rightChild.get("recovery-plugin-properties", "name").asString());
        Assert.assertEquals(rightChild.asString(), "Property6", rightChild.get("recovery-plugin-properties", "name1").asString());

        Assert.assertNotNull("xa-datasource-properties not propagated ", findNodeWithProperty(newList, "value", "jdbc:h2:mem:test"));
    }
}

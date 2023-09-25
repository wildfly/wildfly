/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the situation when abstract DataSource class is specified when creating a data source.
 *
 * @author lgao
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatasourceWrongDsClassTestCase extends JcaMgmtBase {

    private static final String DEPLOYMENT = "dummydriver";

    @Deployment(name = DEPLOYMENT)
    public static JavaArchive jdbcArchive() throws Exception {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar");
        ja.addClasses(DummyDataSource.class, DummyXADataSource.class, TestDriver.class, TestDriver2.class);
        ja.addAsServiceProviderAndClasses(Driver.class, TestDriver.class, TestDriver2.class);
        return ja;
    }

    @Test
    public void testJdbcDrivers() throws Exception {
        String driverName = DEPLOYMENT + ".jar";
        ModelNode address = new ModelNode().add(SUBSYSTEM, "datasources");
        ModelNode operation = new ModelNode();
        operation.get(OP).set("get-installed-driver");
        operation.get(OP_ADDR).set(address);
        operation.get("driver-name").set(driverName);

        ModelNode result = executeOperation(operation).asList().get(0);
        Assert.assertEquals(driverName, result.get("driver-name").asString());
        Assert.assertEquals(driverName, result.get("deployment-name").asString());
        Assert.assertEquals(TestDriver.class.getName(), result.get("driver-class-name").asString());
        Assert.assertEquals(1, result.get("driver-major-version").asInt());
        Assert.assertEquals(0, result.get("driver-minor-version").asInt());
        Assert.assertFalse(result.get("jdbc-compliant").asBoolean());

        String driver1FQCN = driverName + "_" + TestDriver.class.getName() + "_1_0";
        operation.get("driver-name").set(driver1FQCN);
        result = executeOperation(operation).asList().get(0);
        Assert.assertEquals(driver1FQCN, result.get("driver-name").asString());
        Assert.assertEquals(driver1FQCN, result.get("deployment-name").asString());
        Assert.assertEquals(TestDriver.class.getName(), result.get("driver-class-name").asString());
        Assert.assertEquals(1, result.get("driver-major-version").asInt());
        Assert.assertEquals(0, result.get("driver-minor-version").asInt());
        Assert.assertFalse(result.get("jdbc-compliant").asBoolean());

        String driver2FQCN = driverName + "_" + TestDriver2.class.getName() + "_1_1";
        operation.get("driver-name").set(driver2FQCN);
        result = executeOperation(operation).asList().get(0);
        Assert.assertEquals(driver2FQCN, result.get("driver-name").asString());
        Assert.assertEquals(driver2FQCN, result.get("deployment-name").asString());
        Assert.assertEquals(TestDriver2.class.getName(), result.get("driver-class-name").asString());
        Assert.assertEquals(1, result.get("driver-major-version").asInt());
        Assert.assertEquals(1, result.get("driver-minor-version").asInt());
        Assert.assertTrue(result.get("jdbc-compliant").asBoolean());

    }

    private ModelNode getDatasourceAddress(Datasource datasource) {
        ModelNode address = new ModelNode()
            .add(SUBSYSTEM, "datasources")
            .add("data-source", datasource.getName());
        address.protect();
        return address;
    }

    private ModelNode getAddDatasourceOperation(Datasource datasource) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(getDatasourceAddress(datasource));
        operation.get("jndi-name").set(datasource.getJndiName());
        operation.get("driver-name").set(datasource.getDriverName());
        operation.get("enabled").set(datasource.getEnabled());
        operation.get("connection-url").set(datasource.getConnectionUrl());
        return operation;
    }

    private ModelNode getRemoveDatasourceOperation(Datasource ds) {
        ModelNode removeOperation = Operations.createRemoveOperation(getDatasourceAddress(ds));
        removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart")
                .set(true);
        return removeOperation;
    }

    @Test
    public void testDSWithMutipleDrivers() throws Exception {
        String driverName = DEPLOYMENT + ".jar";
        String driver1FQCN = driverName + "_" + TestDriver.class.getName() + "_1_0";
        String driver2FQCN = driverName + "_" + TestDriver2.class.getName() + "_1_1";

        Datasource ds1 = Datasource.Builder("test-ds1").connectionUrl("foo").driverName(driverName).enabled(true)
            .jndiName("java:jboss/datasources/test-ds1")
            .build();
        ModelNode addDS1Operation = getAddDatasourceOperation(ds1);
        try {
            ModelNode addDSResult = getManagementClient().getControllerClient().execute(addDS1Operation);
            Assert.assertEquals("success", addDSResult.get("outcome").asString());
        } finally {
            ModelNode removeDSOperation = getRemoveDatasourceOperation(ds1);
            executeOperation(removeDSOperation);
        }

        Datasource ds1FQCN = Datasource.Builder("test-ds1FQCN").connectionUrl("foo").driverName(driver1FQCN).enabled(true)
                .jndiName("java:jboss/datasources/test-ds1FQCN").build();
        ModelNode addDS1FQCNOperation = getAddDatasourceOperation(ds1FQCN);
        try {
            ModelNode addDSResult = getManagementClient().getControllerClient().execute(addDS1FQCNOperation);
            Assert.assertEquals("success", addDSResult.get("outcome").asString());
        } finally {
            ModelNode removeDSOperation = getRemoveDatasourceOperation(ds1FQCN);
            executeOperation(removeDSOperation);
        }

        Datasource ds2FQCN = Datasource.Builder("test-ds2FQCN").connectionUrl("foo").driverName(driver2FQCN)
                .enabled(true).jndiName("java:jboss/datasources/test-ds2FQCN").build();
        ModelNode addDS2FQCNOperation = getAddDatasourceOperation(ds2FQCN);
        try {
            ModelNode addDSResult = getManagementClient().getControllerClient().execute(addDS2FQCNOperation);
            Assert.assertEquals("success", addDSResult.get("outcome").asString());
        } finally {
            ModelNode removeDSOperation = getRemoveDatasourceOperation(ds2FQCN);
            executeOperation(removeDSOperation);
        }

    }

    @Test
    public void testWrongDSClass() throws Exception {
        String driverName = DEPLOYMENT + ".jar";
        ModelNode address = getDataSourceAddress("wrongClsDs");
        ModelNode operation = getDataSourceOperation(address, "java:/wrongClsDs", driverName, DummyDataSource.class.getName());
        try {
            executeOperation(operation);
            Assert.fail("Not supposed to succeed");
        } catch (MgmtOperationException e) {
            ModelNode result = e.getResult();
            Assert.assertEquals("failed", result.get("outcome").asString());
            String failDesc = result.get("failure-description").asString();
            Assert.assertTrue(failDesc.contains("WFLYJCA0117"));
            return;
        }
        Assert.fail("Not supposed to be here");
    }

    @Test
    public void testWrongXADSClass() throws Exception {
        String driverName = DEPLOYMENT + ".jar";
        ModelNode address = getXADataSourceAddress("wrongXAClsDs");
        ModelNode operation = getXADataSourceOperation(address, "java:/wrongXAClsDs", driverName, DummyXADataSource.class.getName());
        try {
            executeOperation(operation);
            Assert.fail("Not supposed to succeed");
        } catch (MgmtOperationException e) {
            ModelNode result = e.getResult();
            Assert.assertEquals("failed", result.get("outcome").asString());
            return;
        }
        Assert.fail("Not supposed to be here");
    }

    private ModelNode getXADataSourceAddress(String xaDsName) {
        ModelNode address = new ModelNode()
                .add(SUBSYSTEM, "datasources")
                .add("xa-data-source", xaDsName);
        return address;
    }

    private ModelNode getDataSourceAddress(String dsName) {
        ModelNode address = new ModelNode()
                .add(SUBSYSTEM, "datasources")
                .add("data-source", dsName);
        return address;
    }

    private ModelNode getDataSourceOperation(ModelNode address, String jndiName, String driverName, String dsClsName) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get("jndi-name").set(jndiName);
        operation.get("driver-name").set(driverName);
        operation.get("datasource-class").set(dsClsName);
        return operation;
    }

    private ModelNode getXADataSourceOperation(ModelNode address, String jndiName, String driverName, String xaDsClsName) {
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(address);
        addOp.get("jndi-name").set(jndiName);
        addOp.get("driver-name").set(driverName);
        addOp.get("xa-datasource-class").set(xaDsClsName);

        ModelNode connProps = new ModelNode();
        connProps.get(OP).set(ADD);
        ModelNode connPropAdd = address.add("connection-properties", "url");
        connProps.get(OP_ADDR).set(connPropAdd);
        connProps.get("value").set("dummy");
        List<ModelNode> operationList = new ArrayList<>(Arrays.asList(addOp, connProps));
        return ModelUtil.createCompositeNode(operationList.toArray(new ModelNode[1]));
    }
}

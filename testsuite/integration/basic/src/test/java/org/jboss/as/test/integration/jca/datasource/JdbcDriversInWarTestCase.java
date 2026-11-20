/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Driver;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Test the situation when multiple JDBC drivers inside a war.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JdbcDriversInWarTestCase extends JcaMgmtBase {

    private static final String DEPLOYMENT = "jdbc-in-war.war";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive jdbcInWarDeployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT);
        war.addAsLibrary(ShrinkWrap.create(JavaArchive.class, "testDriver1.jar")
                .addClasses(TestDriver.class, DummyDataSource.class, DummyXADataSource.class)
                .addAsServiceProviderAndClasses(Driver.class, TestDriver.class));
        war.addAsLibrary(ShrinkWrap.create(JavaArchive.class, "testDriver2.jar")
                .addClasses(TestDriver2.class, DummyDataSource.class, DummyXADataSource.class)
                .addAsServiceProviderAndClasses(Driver.class, TestDriver2.class));
        return war;
    }

    @Test
    public void testJdbcDrivers() throws Exception {
        String driverName = DEPLOYMENT;
        ModelNode address = new ModelNode().add(SUBSYSTEM, "datasources");
        String driver1FQCN = driverName + "_" + TestDriver.class.getName() + "_1_0";
        ModelNode operation = new ModelNode();
        operation.get(OP).set("get-installed-driver");
        operation.get(OP_ADDR).set(address);
        operation.get("driver-name").set(driver1FQCN);

        ModelNode result = executeOperation(operation).asList().get(0);
        Assertions.assertEquals(driver1FQCN, result.get("driver-name").asString());
        Assertions.assertEquals(driver1FQCN, result.get("deployment-name").asString());
        Assertions.assertEquals(TestDriver.class.getName(), result.get("driver-class-name").asString());
        Assertions.assertEquals(1, result.get("driver-major-version").asInt());
        Assertions.assertEquals(0, result.get("driver-minor-version").asInt());
        Assertions.assertFalse(result.get("jdbc-compliant").asBoolean());

        String driver2FQCN = driverName + "_" + TestDriver2.class.getName() + "_1_1";
        operation.get("driver-name").set(driver2FQCN);
        result = executeOperation(operation).asList().get(0);
        Assertions.assertEquals(driver2FQCN, result.get("driver-name").asString());
        Assertions.assertEquals(driver2FQCN, result.get("deployment-name").asString());
        Assertions.assertEquals(TestDriver2.class.getName(), result.get("driver-class-name").asString());
        Assertions.assertEquals(1, result.get("driver-major-version").asInt());
        Assertions.assertEquals(1, result.get("driver-minor-version").asInt());
        Assertions.assertTrue(result.get("jdbc-compliant").asBoolean());

        // there should not be a driver named with the deployment name
        operation.get("driver-name").set(driverName);
        try {
            executeOperation(operation);
            Assertions.fail("should not be here");
        } catch (MgmtOperationException e) {
            Assertions.assertTrue(e.getResult().get(FAILURE_DESCRIPTION).asString().startsWith("WFLYJCA0135"));
        }
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
        String driverName = DEPLOYMENT;
        String driver1FQCN = driverName + "_" + TestDriver.class.getName() + "_1_0";
        String driver2FQCN = driverName + "_" + TestDriver2.class.getName() + "_1_1";

        Datasource ds1FQCN = Datasource.Builder("test-ds1FQCN").connectionUrl("foo").driverName(driver1FQCN).enabled(true)
                .jndiName("java:jboss/datasources/test-ds1FQCN").build();
        ModelNode addDS1FQCNOperation = getAddDatasourceOperation(ds1FQCN);
        try {
            ModelNode addDSResult = getManagementClient().getControllerClient().execute(addDS1FQCNOperation);
            Assertions.assertEquals("success", addDSResult.get("outcome").asString());
        } finally {
            ModelNode removeDSOperation = getRemoveDatasourceOperation(ds1FQCN);
            executeOperation(removeDSOperation);
        }

        Datasource ds2FQCN = Datasource.Builder("test-ds2FQCN").connectionUrl("foo").driverName(driver2FQCN)
                .enabled(true).jndiName("java:jboss/datasources/test-ds2FQCN").build();
        ModelNode addDS2FQCNOperation = getAddDatasourceOperation(ds2FQCN);
        try {
            ModelNode addDSResult = getManagementClient().getControllerClient().execute(addDS2FQCNOperation);
            Assertions.assertEquals("success", addDSResult.get("outcome").asString());
        } finally {
            ModelNode removeDSOperation = getRemoveDatasourceOperation(ds2FQCN);
            executeOperation(removeDSOperation);
        }

    }

}

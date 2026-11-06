/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import jakarta.resource.cci.Connection;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.shared.logging.LoggingUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the graceful shutdown sequence in
 * {@code AbstractResourceAdapterDeploymentService#unregisterAll}.
 */
@RunWith(Arquillian.class)
public class RaGracefulShutdownTestCase {

    private static final String RAR_NAME     = "graceful-shutdown-test.rar";
    private static final String JNDI_NAME    = "java:jboss/ra-graceful/cf1";
    private static final String POOL_NAME    = "cf1";

    private static final String IJ_DESTROYING_ACTIVE_MSG = "IJ000615";
    private static final String LOG_HANDLER_NAME = "ra-graceful-shutdown-handler";
    private static final String LOG_FILE_NAME    = "ra-graceful-shutdown.log";
    private static final String IJ_POOL_LOGGER   = "org.jboss.jca.core.connectionmanager.pool";

    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_NAME);

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "ra-graceful-shutdown.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage())
          .addClass(RaGracefulShutdownTestCase.class)
          .addClass(LoggingUtil.class);

        raa.addAsLibrary(ja);
        raa.addAsManifestResource(RaGracefulShutdownTestCase.class.getPackage(), "graceful-shutdown-ra.xml", "ra.xml");
        raa.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read"),
                new PropertyPermission("jboss.server.log.dir", "read"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/log/*", "read")
        ), "permissions.xml");

        return raa;
    }

    @ArquillianResource
    private ManagementClient managementClient;

    @Before
    public void setup() throws Exception {
        setupLogging();
        registerResourceAdapter();
    }

    @After
    public void teardown() throws Exception {
        try {
            unregisterResourceAdapter();
        } finally {
            teardownLogging();
        }
    }

    private void setupLogging() throws Exception {
        ModelNode fileHandler = new ModelNode();
        fileHandler.get(OP).set(ADD);
        fileHandler.get(OP_ADDR).add("subsystem", "logging").add("file-handler", LOG_HANDLER_NAME);
        ModelNode file = new ModelNode();
        file.get("relative-to").set("jboss.server.log.dir");
        file.get("path").set(LOG_FILE_NAME);
        fileHandler.get("file").set(file);
        fileHandler.get("append").set(false);
        fileHandler.get("level").set("WARN");
        executeAndAssertSuccess(fileHandler);

        ModelNode logger = new ModelNode();
        logger.get(OP).set(ADD);
        logger.get(OP_ADDR).add("subsystem", "logging").add("logger", IJ_POOL_LOGGER);
        logger.get("level").set("WARN");
        logger.get("handlers").add(LOG_HANDLER_NAME);
        executeAndAssertSuccess(logger);
    }

    private void teardownLogging() throws Exception {
        ModelNode removeLogger = new ModelNode();
        removeLogger.get(OP).set(REMOVE_OPERATION);
        removeLogger.get(OP_ADDR).add("subsystem", "logging").add("logger", IJ_POOL_LOGGER);
        managementClient.getControllerClient().execute(removeLogger);

        ModelNode removeHandler = new ModelNode();
        removeHandler.get(OP).set(REMOVE_OPERATION);
        removeHandler.get(OP_ADDR).add("subsystem", "logging").add("file-handler", LOG_HANDLER_NAME);
        managementClient.getControllerClient().execute(removeHandler);
    }

    private void registerResourceAdapter() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("subsystem", "resource-adapters")
                       .add("resource-adapter", RAR_NAME);
        op.get("archive").set(RAR_NAME);
        op.get("transaction-support").set("NoTransaction");
        executeAndAssertSuccess(op);

        ModelNode cdOp = new ModelNode();
        cdOp.get(OP).set(ADD);
        cdOp.get(OP_ADDR).add("subsystem", "resource-adapters")
                         .add("resource-adapter", RAR_NAME)
                         .add("connection-definitions", POOL_NAME);
        cdOp.get("class-name").set("org.jboss.as.test.integration.jca.rar.MultipleManagedConnectionFactory1");
        cdOp.get("jndi-name").set(JNDI_NAME);
        executeAndAssertSuccess(cdOp);

        ModelNode activateOp = new ModelNode();
        activateOp.get(OP).set("activate");
        activateOp.get(OP_ADDR).add("subsystem", "resource-adapters")
                               .add("resource-adapter", RAR_NAME);
        executeAndAssertSuccess(activateOp);
    }

    private void unregisterResourceAdapter() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE_OPERATION);
        op.get(OP_ADDR).add("subsystem", "resource-adapters")
                       .add("resource-adapter", RAR_NAME);
        op.get("operation-headers", "allow-resource-service-restart").set(true);
        managementClient.getControllerClient().execute(op);
    }

    @Test
    public void testUndeployWithActiveConnectionSucceeds() throws Exception {
        MultipleConnectionFactory1 cf =
                (MultipleConnectionFactory1) new InitialContext().lookup(JNDI_NAME);
        Assert.assertNotNull("Connection factory must be bound in JNDI", cf);

        Connection openConnection = cf.getConnection();
        Assert.assertNotNull("A connection must be allocated", openConnection);

        ModelNode removeOp = new ModelNode();
        removeOp.get(OP).set(REMOVE_OPERATION);
        removeOp.get(OP_ADDR).add("subsystem", "resource-adapters")
                             .add("resource-adapter", RAR_NAME);
        removeOp.get("operation-headers", "allow-resource-service-restart").set(true);

        ModelNode result = managementClient.getControllerClient().execute(removeOp);
        Assert.assertEquals(
                "Remove with an active connection must succeed. Full result: " + result,
                SUCCESS, result.get(OUTCOME).asString());

        Assert.assertFalse(
                "IJ000615 (Destroying active connection in pool) must NOT appear in the server log.",
                LoggingUtil.hasLogMessage(LOG_FILE_NAME, IJ_DESTROYING_ACTIVE_MSG));
    }

    private void executeAndAssertSuccess(ModelNode op) throws Exception {
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertEquals(
                "Operation failed: " + result,
                SUCCESS, result.get(OUTCOME).asString());
    }
}
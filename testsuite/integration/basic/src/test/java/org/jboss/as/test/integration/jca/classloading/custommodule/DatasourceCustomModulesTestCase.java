/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2021, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.as.test.integration.jca.classloading.custommodule;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnection;
import org.jboss.jca.adapters.jdbc.WrappedConnection;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(DatasourceCustomModulesTestCase.Setup.class)
public class DatasourceCustomModulesTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    private PathAddress dsAddress;
    private String dsName;
    private String dsJndiName;
    private DataSource dataSource;

    private List<Connection> connectionList;
    private List<Connection> physicalConnectionList;

    private ClassLoader customModuleClassloader;

    @Test
    public void testModuleValidationClasses() throws Exception {

        createDatasource();

        initConnections(3);

        TestValidConnectionChecker.reset();
        TestExceptionSorter.reset();
        TestStaleConnectionChecker.reset();

        // reflection hack to induce connection error validation
        Connection connection = connectionList.get(0);
        Field managedConnectionField = WrappedConnection.class.getDeclaredField("mc");
        managedConnectionField.setAccessible(true);
        Object managedConnection = managedConnectionField.get(connection);
        Method connectionErrorMethod = BaseWrapperManagedConnection.class.getDeclaredMethod("connectionError", Throwable.class);
        connectionErrorMethod.setAccessible(true);
        connectionErrorMethod.invoke(managedConnection, new SQLException());

        connectionList.get(0).close();
        connectionList.get(1).close();

        // flush operation to induce connection validation
        runDataSourceOperationAndAssertSuccess("flush-invalid-connection-in-pool");


        Assert.assertTrue(TestValidConnectionChecker.wasInvoked());
        Assert.assertTrue(TestExceptionSorter.wasInvoked());
        Assert.assertTrue(TestStaleConnectionChecker.wasInvoked());


    }

    public void initConnections(int count) {
        connectionList = IntStream.range(0, count)
                .mapToObj(i -> connectionSupplier.apply(dataSource))
                .collect(Collectors.toList());

        physicalConnectionList = connectionList
                .stream()
                .map(this::getUnderlyingConnection)
                .collect(Collectors.toList());
    }

    public Connection getUnderlyingConnection(Connection handle) {
        try {
            return ((WrappedConnection) handle).getUnderlyingConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Function<DataSource, Connection> connectionSupplier = (dataSource -> {
        try {
            return dataSource.getConnection("sa", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    });

    public void runDataSourceOperationAndAssertSuccess(String operationName) throws IOException {
        final ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        op.get(OP_ADDR).set(dsAddress.toModelNode());
        executeAndAssertSuccess(op);
    }

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "flush-operations.jar");
        archive.addClass(DatasourceCustomModulesTestCase.class);
        archive.addAsManifestResource(
                new StringAsset(
                        "Dependencies: org.jboss.as.controller-client, "
                                + "org.jboss.as.controller, "
                                + "org.jboss.dmr, "
                                + "org.jboss.ironjacamar.jdbcadapters, "
                                + "org.jboss.test.customModule, "
                                + "org.jboss.remoting\n"),
                "MANIFEST.MF");
        archive.addAsManifestResource(createPermissionsXmlAsset(
                // ModelControllerClient needs the following
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                // flushInvalidConnectionsInPool needs the following
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        return archive;
    }

    public void createDatasource() throws Exception {
        dsName = java.util.UUID.randomUUID().toString();
        dsAddress = PathAddress.pathAddress(SUBSYSTEM, "datasources").append("data-source", dsName);
        dsJndiName = "java:/" + dsName;

        final ModelNode createDsOp = new ModelNode();
        createDsOp.get(OP).set(ADD);
        createDsOp.get(OP_ADDR).set(dsAddress.toModelNode());
        createDsOp.get("jndi-name").set(dsJndiName);
        createDsOp.get("driver-name").set("h2");
        createDsOp.get("valid-connection-checker-class-name").set(TestValidConnectionChecker.class.getName());
        createDsOp.get("valid-connection-checker-module").set("org.jboss.test.customModule");
        createDsOp.get("exception-sorter-class-name").set(TestExceptionSorter.class.getName());
        createDsOp.get("exception-sorter-module").set("org.jboss.test.customModule");
        createDsOp.get("stale-connection-checker-class-name").set(TestStaleConnectionChecker.class.getName());
        createDsOp.get("stale-connection-checker-module").set("org.jboss.test.customModule");
        createDsOp.get("min-pool-size").set(2);
        createDsOp.get("max-pool-size").set(5);

        createDsOp.get("connection-url").set("jdbc:h2:mem:test42");
        executeAndAssertSuccess(createDsOp);

        dataSource = getDataSourceInstanceFromJndi();
    }

    public void cleanup() throws Exception {
        if (connectionList != null) {
            connectionList.forEach(this::closeIfNecessary);
            connectionList = null;
        }

        final ModelNode removeOp = new ModelNode();
        removeOp.get(OP).set(REMOVE_OPERATION);
        removeOp.get(OP_ADDR).set(dsAddress.toModelNode());
        removeOp.get(OPERATION_HEADERS, ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        executeAndAssertSuccess(removeOp);
    }

    public void executeAndAssertSuccess(ModelNode op) throws IOException {
        final ModelNode result = managementClient.getControllerClient().execute(op);
        if (!result.get(OUTCOME).asString().equals(SUCCESS)) {
            Assert.fail("Operation failed: " + result.toJSONString(true));
        }
    }

    public DataSource getDataSourceInstanceFromJndi() throws NamingException {
        return (DataSource) new InitialContext().lookup(dsJndiName);
    }

    public void closeIfNecessary(Connection connection) {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class Setup implements ServerSetupTask {

        private TestModule customModule;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            TestModule customModule = new TestModule("org.jboss.test.customModule",
                    new String[] {"java.sql", "javax.orb.api", "java.logging", "org.jboss.ironjacamar.jdbcadapters"});
            customModule.addResource("customModule.jar")
                    .addClasses(TestValidConnectionChecker.class, TestExceptionSorter.class, TestStaleConnectionChecker.class);

            customModule.create();

            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            if (customModule != null) {
                customModule.remove();
            }
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }
    }
}

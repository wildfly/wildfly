/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.flushing;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ReflectPermission;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.WrappedConnection;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test for flush operations on data source pools:
 * - flush-all-connection-in-pool
 * - flush-idle-connection-in-pool
 * - flush-invalid-connection-in-pool
 * - flush-gracefully-connection-in-pool
 * <p>
 * Everything is tested with allow-multiple-users=true.
 * <p>
 * For testing whether a pooled connection is really closed, we check whether the underlying connection is closed.
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
public class FlushOperationsTestCase {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "flush-operations.jar");
        archive.addClass(FlushOperationsTestCase.class);
        archive.addAsManifestResource(
                new StringAsset(
                        "Dependencies: org.jboss.as.controller-client, "
                                + "org.jboss.as.controller, "
                                + "org.jboss.dmr, "
                                + "org.jboss.ironjacamar.jdbcadapters, "
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

    @ArquillianResource
    private ManagementClient managementClient;

    private PathAddress dsAddress;
    private String dsName;
    private String dsJndiName;
    private DataSource dataSource;

    private List<Connection> connectionList;
    private List<Connection> physicalConnectionList;

    @Before
    public void createDatasource() throws Exception {
        dsName = java.util.UUID.randomUUID().toString();
        dsAddress = PathAddress.pathAddress(SUBSYSTEM, "datasources").append("data-source", dsName);
        dsJndiName = "java:/" + dsName;

        final ModelNode createDsOp = new ModelNode();
        createDsOp.get(OP).set(ADD);
        createDsOp.get(OP_ADDR).set(dsAddress.toModelNode());
        createDsOp.get("jndi-name").set(dsJndiName);
        createDsOp.get("valid-connection-checker-class-name")
                .set("org.jboss.jca.adapters.jdbc.extensions.novendor.JDBC4ValidConnectionChecker");
        createDsOp.get("driver-name").set("h2");
        createDsOp.get("allow-multiple-users").set(true);
        createDsOp.get("connection-url").set("jdbc:h2:mem:test42");
        executeAndAssertSuccess(createDsOp);

        dataSource = getDataSourceInstanceFromJndi();
    }

    /**
     * After each test:
     * - close all connections in case that some remained open
     * - delete the data source
     */
    @After
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

    /**
     * Obtain 6 connections.
     * Return connections 0,1,2 to the pool.
     * Flush all connections.
     * All connections, especially 3,4,5; should be closed after the flush.
     */
    @Test
    public void flushAllConnectionsInPool() throws NamingException, SQLException, IOException {
        initConnections(6);

        connectionList.get(0).close();
        connectionList.get(1).close();
        connectionList.get(2).close();

        runDataSourceOperationAndAssertSuccess("flush-all-connection-in-pool");

        for (int i = 0; i < connectionList.size(); i++) {
            Assert.assertTrue("Connection #" + i + " should be flushed",
                    physicalConnectionList.get(i).isClosed());
        }
    }

    /**
     * Obtain 2 connections.
     * Return connections 0 to the pool.
     * Flush all connections gracefully.
     * Check that only connections 0 is closed while 1 remains open.
     * Return connections 3,4,5 to the pool.
     * Check all connections 3,4,5 were closed now.
     */
    @Test
    public void flushGracefullyConnectionsInPool() throws Exception {
        initConnections(6);

        connectionList.get(0).close();
        connectionList.get(1).close();
        connectionList.get(2).close();

        runDataSourceOperationAndAssertSuccess("flush-gracefully-connection-in-pool");

        Assert.assertTrue("Connection 0 should get destroyed immediately because it is idle",
                physicalConnectionList.get(0).isClosed());
        Assert.assertTrue("Connection 1 should get destroyed immediately because it is idle",
                physicalConnectionList.get(1).isClosed());
        Assert.assertTrue("Connection 2 should get destroyed immediately because it is idle",
                physicalConnectionList.get(2).isClosed());
        Assert.assertFalse("Connection 3 should not get destroyed immediately because it is not idle",
                physicalConnectionList.get(3).isClosed());
        Assert.assertFalse("Connection 4 should not get destroyed immediately because it is not idle",
                physicalConnectionList.get(4).isClosed());
        Assert.assertFalse("Connection 5 should not get destroyed immediately because it is not idle",
                physicalConnectionList.get(5).isClosed());

        connectionList.get(3).close();
        connectionList.get(4).close();
        connectionList.get(5).close();

        for (int i = 0; i < connectionList.size(); i++) {
            Assert.assertTrue("Connection #" + i + " should be flushed",
                    physicalConnectionList.get(i).isClosed());
        }
    }

    /**
     * Obtain 6 connections.
     * Return connections 0,1,2 to the pool.
     * Run flush-idle-connections-in-pool
     * Check that only the physical connections corresponding to handles 0,1,2 were destroyed.
     */
    @Test
    public void flushIdleConnectionsInPool() throws Exception {
        initConnections(6);

        connectionList.get(0).close();
        connectionList.get(1).close();
        connectionList.get(2).close();

        runDataSourceOperationAndAssertSuccess("flush-idle-connection-in-pool");

        Assert.assertTrue("Connection 0 should get destroyed because it is idle",
                physicalConnectionList.get(0).isClosed());
        Assert.assertTrue("Connection 1 should get destroyed because it is idle",
                physicalConnectionList.get(1).isClosed());
        Assert.assertTrue("Connection 2 should get destroyed because it is idle",
                physicalConnectionList.get(2).isClosed());
        Assert.assertFalse("Connection 3 should not get destroyed because it is not idle",
                physicalConnectionList.get(3).isClosed());
        Assert.assertFalse("Connection 4 should not get destroyed because it is not idle",
                physicalConnectionList.get(4).isClosed());
        Assert.assertFalse("Connection 5 should not get destroyed because it is not idle",
                physicalConnectionList.get(5).isClosed());
    }


    /**
     * Obtain 3 connections.
     * Return connections 0,1.
     * Mark connection 0 as invalid
     * Run flush-invalid-connection-in-pool
     * Managed connection of connection 0 should be flushed
     * Managed connection of connection 1 should NOT be flushed because the connection was not marked invalid
     * Managed connection of connection 2 should NOT be flushed because the connection was not idle
     */
    @Test
    public void flushInvalidConnectionsInPool() throws Exception {
        initConnections(3);

        connectionList.get(0).close();
        connectionList.get(1).close();

        // make connection 0 invalid somehow - for example, hack its session field to null
        // this only works with H2
        final Field sessionField = physicalConnectionList.get(0).getClass().getDeclaredField("session");
        try {
            sessionField.setAccessible(true);
            sessionField.set(physicalConnectionList.get(0), null);
        } finally {
            sessionField.setAccessible(false);
        }

        runDataSourceOperationAndAssertSuccess("flush-invalid-connection-in-pool");

        Assert.assertTrue("Connection 0 should get destroyed because it is marked invalid",
                physicalConnectionList.get(0).isClosed());
        Assert.assertFalse("Connection 1 should not get destroyed because it is valid",
                physicalConnectionList.get(1).isClosed());
        Assert.assertFalse("Connection 2 should not get destroyed because it is not idle",
                physicalConnectionList.get(2).isClosed());
    }

    /**
     * Creates a number of connections using the tested data source.
     * Stores the connections into a list for reference.
     */
    public void initConnections(int count) {
        connectionList = IntStream.range(0, count)
                .mapToObj(i -> connectionSupplier.apply(dataSource))
                .collect(Collectors.toList());

        physicalConnectionList = connectionList
                .stream()
                .map(this::getUnderlyingConnection)
                .collect(Collectors.toList());
    }

    public void executeAndAssertSuccess(ModelNode op) throws IOException {
        final ModelNode result = managementClient.getControllerClient().execute(op);
        if (!result.get(OUTCOME).asString().equals(SUCCESS)) {
            Assert.fail("Operation failed: " + result.toJSONString(true));
        }
    }

    public void runDataSourceOperationAndAssertSuccess(String operationName) throws IOException {
        final ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        op.get(OP_ADDR).set(dsAddress.toModelNode());
        executeAndAssertSuccess(op);
    }

    public DataSource getDataSourceInstanceFromJndi() throws NamingException {
        return (DataSource)new InitialContext().lookup(dsJndiName);
    }

    public Function<DataSource, Connection> connectionSupplier = (dataSource -> {
        try {
            return dataSource.getConnection("sa", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    });

    public void closeIfNecessary(Connection connection) {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getUnderlyingConnection(Connection handle) {
        try {
            return ((WrappedConnection)handle).getUnderlyingConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
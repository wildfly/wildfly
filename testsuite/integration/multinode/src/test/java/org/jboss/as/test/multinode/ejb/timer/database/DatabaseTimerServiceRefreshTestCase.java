/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.multinode.ejb.timer.database;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.multinode.ejb.timer.database.DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.getRemoteContext;
import static org.jboss.as.test.multinode.ejb.timer.database.RefreshIF.Info.CLIENT1;
import static org.jboss.as.test.multinode.ejb.timer.database.RefreshIF.Info.RETURN_HANDLE;
import static org.jboss.as.test.multinode.ejb.timer.database.RefreshIF.Info.SERVER1;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.SocketPermission;
import java.security.SecurityPermission;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;

import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DatabaseTimerServiceRefreshTestCase.DatabaseTimerServiceTestCaseServerSetup.class)
public class DatabaseTimerServiceRefreshTestCase {
    private static final String ARCHIVE_NAME = "testTimerServiceRefresh";
    private static Server server;
    private static final long TIMER_DELAY = TimeUnit.MINUTES.toMillis(20);

    static final PathAddress ADDR_DATA_SOURCE = PathAddress.pathAddress().append(SUBSYSTEM, "datasources").append("data-source", "MyNewDs");
    static final PathAddress ADDR_DATA_STORE = PathAddress.pathAddress().append(SUBSYSTEM, "ejb3").append(SERVICE, "timer-service").append("database-data-store", "dbstore");

    @AfterClass
    public static void afterClass() {
        if (server != null) {
            server.stop();
        }
    }

    static class DatabaseTimerServiceTestCaseServerSetup implements ServerSetupTask {

        private static final PathAddress ADDR_DATA_SOURCE = PathAddress.pathAddress().append(SUBSYSTEM, "datasources").append("data-source", "MyNewDs");
        private static final PathAddress ADDR_TIMER_SERVICE = PathAddress.pathAddress().append(SUBSYSTEM, "ejb3").append("service", "timer-service");
        private static final PathAddress ADDR_DATABASE_DATA_STORE = ADDR_TIMER_SERVICE.append("database-data-store", "dbstore");

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            if (server == null) {
                //we need a TCP server that can be shared between the two servers
                server = Server.createTcpServer().start();
            }

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /subsystem=datasources/data-source=MyNewDs:add(name=MyNewDs,jndi-name=java:jboss/datasources/TimeDs, enabled=true)
            ModelNode datasourceAddModelNode = Util.createAddOperation(ADDR_DATA_SOURCE);
            datasourceAddModelNode.get("name").set("MyNewDs");
            datasourceAddModelNode.get("jndi-name").set("java:jboss/datasources/TimeDs");
            datasourceAddModelNode.get("enabled").set(true);
            datasourceAddModelNode.get("driver-name").set("h2");
            datasourceAddModelNode.get("pool-name").set("MyNewDs_Pool");
            datasourceAddModelNode.get("connection-url").set("jdbc:h2:" + server.getURL() + "/mem:testdb;DB_CLOSE_DELAY=-1");
            datasourceAddModelNode.get("user-name").set("sa");
            datasourceAddModelNode.get("password").set("sa");
            steps.add(datasourceAddModelNode);

            // /subsystem=ejb3/service=timer-service/database-data-store=dbstore:add(odatabase-jndi-name=java:jboss/datrasources/TimeDs)
            ModelNode databaseDataStoreAddModelNode = Util.createAddOperation(ADDR_DATABASE_DATA_STORE);
            databaseDataStoreAddModelNode.get("datasource-jndi-name").set("java:jboss/datasources/TimeDs");
            databaseDataStoreAddModelNode.get("database").set("postgresql");
            databaseDataStoreAddModelNode.get("refresh-interval").set(TimeUnit.MINUTES.toMillis(30));
            steps.add(databaseDataStoreAddModelNode);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            ModelNode databaseDataStoreRemoveModelNode = Util.createRemoveOperation(ADDR_DATABASE_DATA_STORE);
            // omitting op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false)
            steps.add(databaseDataStoreRemoveModelNode);

            ModelNode datasourceRemoveModelNode = Util.createRemoveOperation(ADDR_DATA_SOURCE);
            // omitting op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false)
            steps.add(datasourceRemoveModelNode);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }
    }

    @ContainerResource("multinode-server")
    private ManagementClient serverClient;

    @ContainerResource("multinode-client")
    private ManagementClient clientClient;

    @Deployment(name = "server", testable = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment() {
        return createDeployment(false);
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("multinode-client")
    public static Archive<?> clientDeployment() {
        return createDeployment(true);
    }

    private static Archive<?> createDeployment(boolean client) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(RefreshInterceptor.class, RefreshIF.class, RefreshBeanBase.class, RefreshBean1.class, RefreshBean2.class);
        war.addAsWebInfResource(DatabaseTimerServiceRefreshTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");

        if (client) {
            war.addAsManifestResource(DatabaseTimerServiceRefreshTestCase.class.getPackage(), "jboss-ejb-client-refresh-test.xml", "jboss-ejb-client.xml");
            war.addAsManifestResource(
                    createPermissionsXmlAsset(
                            new SocketPermission("*:9092", "connect,resolve"),
                            new SecurityPermission("putProviderProperty.WildFlyElytron")),
                    "permissions.xml");
        }
        return war;
    }

    /**
     * Verifies that application can programmatically refresh timers across different nodes,
     * when programmatic refresh is enabled through interceptor.
     */
    @Test
    public void testTimerProgrammaticRefresh() throws Exception {
        Context clientContext = null;
        Context serverContext = null;

        try {
            clientContext = getRemoteContext(clientClient);
            serverContext = getRemoteContext(serverClient);

            RefreshIF bean1Client = (RefreshIF) clientContext.lookup(ARCHIVE_NAME + "/" + RefreshBean1.class.getSimpleName() + "!" + RefreshIF.class.getName());
            RefreshIF bean2Client = (RefreshIF) clientContext.lookup(ARCHIVE_NAME + "/" + RefreshBean2.class.getSimpleName() + "!" + RefreshIF.class.getName());

            RefreshIF bean1Server = (RefreshIF) serverContext.lookup(ARCHIVE_NAME + "/" + RefreshBean1.class.getSimpleName() + "!" + RefreshIF.class.getName());
            RefreshIF bean2Server = (RefreshIF) serverContext.lookup(ARCHIVE_NAME + "/" + RefreshBean2.class.getSimpleName() + "!" + RefreshIF.class.getName());

            RefreshIF[] serverBeans = {bean1Server, bean2Server};
            RefreshIF[] clientBeans = {bean1Client, bean2Client};

            // client bean 1 creates a timer in client node
            // both client beans will see this newly-created timer
            bean1Client.createTimer(TIMER_DELAY, CLIENT1);
            for (RefreshIF b : clientBeans) {
                verifyTimerInfo(b.getAllTimerInfoNoRefresh(), 1, CLIENT1);
            }
            // without refresh, both server beans see no timer
            for (RefreshIF b : serverBeans) {
                verifyTimerInfo(b.getAllTimerInfoNoRefresh(), 0);
            }
            // after server bean 1 refreshes, both server beans see the timer created in client node
            verifyTimerInfo(bean1Server.getAllTimerInfoWithRefresh(), 1, CLIENT1);
            verifyTimerInfo(bean2Server.getAllTimerInfoNoRefresh(), 1, CLIENT1);

            // after cancelling the timer, client beans see no timer
            // but both server beans still see 1 obsolete timer
            bean1Client.cancelTimers();
            for (RefreshIF b : clientBeans) {
                verifyTimerInfo(b.getAllTimerInfoNoRefresh(), 0);
            }
            for (RefreshIF b : serverBeans) {
                verifyTimerInfo(b.getAllTimerInfoNoRefresh(), 1, CLIENT1);
            }

            // after server bean 2 refreshes, both server beans see no timer
            verifyTimerInfo(bean2Server.getAllTimerInfoWithRefresh2(), 0);
            verifyTimerInfo(bean1Server.getAllTimerInfoNoRefresh(), 0);

            // after server bean 1 creates a timer, both client beans see no timer
            // after client bean 2 refreshes, both client beans see this timer
            bean1Server.createTimer(TIMER_DELAY, SERVER1);
            for (RefreshIF b : clientBeans) {
                verifyTimerInfo(b.getAllTimerInfoNoRefresh(), 0);
            }
            verifyTimerInfo(bean2Client.getAllTimerInfoWithRefresh(), 1, SERVER1);
            verifyTimerInfo(bean1Client.getAllTimerInfoNoRefresh(), 1, SERVER1);

            // server bean 1 cancels this timer
            // after refresh, both client beans see no timer
            bean1Server.cancelTimers();
            verifyTimerInfo(bean1Client.getAllTimerInfoWithRefresh2(), 0);
            verifyTimerInfo(bean2Client.getAllTimerInfoNoRefresh(), 0);

            // after server bean 1 creates a timer, both client beans see no timer,
            // but client bean 1 should still be able to cancel this timer.
            final byte[] handle = bean1Server.createTimer(TIMER_DELAY, RETURN_HANDLE);
            try {
                bean1Client.cancelTimer(handle);
                verifyTimerInfo(bean1Server.getAllTimerInfoWithRefresh(), 0);
                verifyTimerInfo(bean1Client.getAllTimerInfoWithRefresh(), 0);
            } finally {
                //clean up (cancel in the active node) in case the timer was not cancelled by client bean 1
                try {
                    bean1Server.cancelTimers();
                } catch (Exception ignore) {
                }
            }
        } finally {
            if (clientContext != null) {
                clientContext.close();
            }
            if (serverContext != null) {
                serverContext.close();
            }
        }
    }

    private void verifyTimerInfo(List<Serializable> infoList, int expectedSize, Serializable... expectedInfo) {
        assertEquals(expectedSize, infoList.size());
        for (Serializable e : expectedInfo) {
            assertTrue("Expecting timer info: " + e + " not found in timer info list: " + infoList, infoList.contains(e));
        }
    }
}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.multinode.ejb.timer.database.DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.getRemoteContext;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.security.SecurityPermission;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that timers are never doubled up
 *
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DatabaseTimerServiceMultiNodeTestCase.DatabaseTimerServiceTestCaseServerSetup.class)
public class DatabaseTimerServiceMultiNodeTestCase {

    public static final String ARCHIVE_NAME = "testTimerServiceSimple";
    public static final int TIMER_COUNT = 100;
    private static Server server;

    private static final int TIMER_DELAY = 400;

    static final PathAddress ADDR_DATA_SOURCE = PathAddress.pathAddress().append(SUBSYSTEM, "datasources").append("data-source", "MyNewDs");
    static final PathAddress ADDR_DATA_STORE = PathAddress.pathAddress().append(SUBSYSTEM, "ejb3").append(ModelDescriptionConstants.SERVICE, "timer-service").append("database-data-store", "dbstore");

    @AfterClass
    public static void afterClass() {
        if(server != null) {
            server.stop();
        }
    }

    static class DatabaseTimerServiceTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            if(server == null) {
                //we need a TCP server that can be shared between the two servers
                server = Server.createTcpServer().start();
            }

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // add a datasource at /subsystem=datasources/data-source=MyNewDs with appropriate attributes
            final ModelNode addDataSource = Util.createAddOperation(ADDR_DATA_SOURCE);
            addDataSource.get("name").set("MyNewDs");
            addDataSource.get("jndi-name").set("java:jboss/datasources/TimeDs");
            addDataSource.get("enabled").set(true);
            addDataSource.get("driver-name").set("h2");
            addDataSource.get("pool-name").set("MyNewDs_Pool");
            addDataSource.get("connection-url").set("jdbc:h2:" + server.getURL() + "/mem:testdb;DB_CLOSE_DELAY=-1");
            addDataSource.get("user-name").set("sa");
            addDataSource.get("password").set("sa");

            steps.add(addDataSource);

            // add a datastore at /subsystem=ejb3/service=timerservice/database-data-store=dbstore with appropriate attributes
            final ModelNode addDataStore = Util.createAddOperation(ADDR_DATA_STORE);
            addDataStore.get("datasource-jndi-name").set("java:jboss/datasources/TimeDs");
            addDataStore.get("database").set("postgresql");
            addDataStore.get("refresh-interval").set(100);
            addDataStore.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            steps.add(addDataStore);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            final ModelNode removeDataStore = Util.createRemoveOperation(ADDR_DATA_STORE);
            removeDataStore.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            removeDataStore.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            steps.add(removeDataStore);

            final ModelNode removeDataSource = Util.createRemoveOperation(ADDR_DATA_SOURCE);
            removeDataSource.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            removeDataSource.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            steps.add(removeDataSource);

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
        war.addClasses(Collector.class, RemoteTimedBean.class, TimedObjectTimerServiceBean.class, TimerData.class, FileUtils.class);
        war.addAsWebInfResource(DatabaseTimerServiceMultiNodeTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        if(!client) {
            war.addClass(CollectionSingleton.class);
        }
        war.addAsResource(new StringAsset(client ? "client" : "server"), "node.txt");
        if (client) {
            war.addAsManifestResource(DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
            war.addAsManifestResource(
                    createPermissionsXmlAsset(
                            new SocketPermission("*:9092", "connect,resolve"),
                            new SecurityPermission("putProviderProperty.WildFlyElytron")),
                    "permissions.xml");
        }
        return war;
    }

    @Test
    public void testEjbTimeoutOnOtherNode() throws Exception {

        Context clientContext = getRemoteContext(clientClient);
        try {
            RemoteTimedBean clientBean = (RemoteTimedBean) clientContext.lookup(ARCHIVE_NAME + "/" + TimedObjectTimerServiceBean.class.getSimpleName() + "!" + RemoteTimedBean.class.getName());
            Set<String> names = new HashSet<>();
            long time = System.currentTimeMillis() + TIMER_DELAY;
            for (int i = 0; i < TIMER_COUNT; ++i) {
                String name = "timer" + i;
                clientBean.scheduleTimer(time, name);
                names.add(name);
            }

            final Context remoteContext = getRemoteContext(serverClient);
            try {
                Collector serverBean = (Collector) remoteContext.lookup(ARCHIVE_NAME + "/" + CollectionSingleton.class.getSimpleName() + "!" + Collector.class.getName());
                List<TimerData> res = serverBean.collect(TIMER_COUNT);
                Assert.assertEquals("Expected " + TIMER_COUNT + " was " + res.size() + " " + res, TIMER_COUNT, res.size());
                boolean server = false;
                boolean client = false;
                final Set<String> newNames = new HashSet<>(names);
                for (TimerData r : res) {
                    if (!newNames.remove(r.getInfo())) {
                        if (!names.contains(r.getInfo())) {
                            throw new RuntimeException("Timer " + r.getInfo() + " not run " + res);
                        } else {
                            throw new RuntimeException("Timer " + r.getInfo() + " run twice " + res);
                        }
                    }
                    if (r.getNode().equals("client")) {
                        client = true;
                    } else if (r.getNode().equals("server")) {
                        server = true;
                    }
                }
                Assert.assertTrue(client);
                Assert.assertTrue(server);
            } finally {
                remoteContext.close();
            }
        } finally {
            clientContext.close();
        }
    }
}

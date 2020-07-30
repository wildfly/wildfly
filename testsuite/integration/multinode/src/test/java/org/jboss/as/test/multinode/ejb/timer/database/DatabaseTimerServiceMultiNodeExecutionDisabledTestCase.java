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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

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
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
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
 * Tests that a timer created on a node with timeout disabled can be run on a different node in the cluster.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.DatabaseTimerServiceTestCaseServerSetup.class)
public class DatabaseTimerServiceMultiNodeExecutionDisabledTestCase {

    public static final String ARCHIVE_NAME = "testTimerServiceSimple";
    private static Server server;

    static final PathAddress ADDR_DATA_SOURCE = PathAddress.pathAddress().append(SUBSYSTEM, "datasources").append("data-source", "MyNewDs_disabled");
    static final PathAddress ADDR_DATA_STORE = PathAddress.pathAddress().append(SUBSYSTEM, "ejb3").append(SERVICE, "timer-service").append("database-data-store", "dbstore");

    @AfterClass
    public static void afterClass() {
        if (server != null) {
            server.stop();
        }
    }

    static class DatabaseTimerServiceTestCaseServerSetup implements ServerSetupTask {

        private static final PathAddress ADDR_DATA_SOURCE = PathAddress.pathAddress().append(SUBSYSTEM, "datasources").append("data-source", "MyNewDs_disabled");
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

            // /subsystem=datasources/data-source=MyNewDs_disabled:add(name=MyNewDs_disabled,jndi-name=java:jboss/datasources/TimeDs_disabled, enabled=true,...)
            ModelNode datasourceAddModelNode = Util.createAddOperation(ADDR_DATA_SOURCE);
            datasourceAddModelNode.get("name").set("MyNewDs_disabled");
            datasourceAddModelNode.get("jndi-name").set("java:jboss/datasources/TimeDs_disabled");
            datasourceAddModelNode.get("enabled").set(true);
            datasourceAddModelNode.get("driver-name").set("h2");
            datasourceAddModelNode.get("pool-name").set("MyNewDs_disabled_Pool");
            datasourceAddModelNode.get("connection-url").set("jdbc:h2:" + server.getURL() + "/mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            datasourceAddModelNode.get("user-name").set("sa");
            datasourceAddModelNode.get("password").set("sa");
            steps.add(datasourceAddModelNode);

            // /subsystem=ejb3/service=timer-service/database-data-store=dbstore:add(odatabase-jndi-name=java:jboss/datrasources/TimeDs,...)
            ModelNode databaseDataStoreAddModelNode = Util.createAddOperation(ADDR_DATABASE_DATA_STORE);
            databaseDataStoreAddModelNode.get("datasource-jndi-name").set("java:jboss/datasources/TimeDs_disabled");
            databaseDataStoreAddModelNode.get("database").set("postgresql");
            if (containerId.equals("multinode-client")) {
                databaseDataStoreAddModelNode.get("allow-execution").set(false);
            }
            databaseDataStoreAddModelNode.get("refresh-interval").set(100);
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
        war.addClasses(Collector.class, RemoteTimedBean.class, TimedObjectTimerServiceBean.class, TimerData.class, FileUtils.class);
        if (!client) {
            war.addClass(CollectionSingleton.class);
        }
        String nodeName = client ? "client" : "server";
        war.addAsResource(new StringAsset(nodeName), "node.txt");
        war.addAsWebInfResource(DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        if (client) {
            war.addAsManifestResource(DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        }
        return war;
    }

    @Test
    public void testEjbTimeoutOnOtherNode() throws Exception {

        Context clientContext = getRemoteContext(clientClient);
        RemoteTimedBean clientBean = (RemoteTimedBean) clientContext.lookup(ARCHIVE_NAME + "/" + TimedObjectTimerServiceBean.class.getSimpleName() + "!" + RemoteTimedBean.class.getName());

        clientBean.scheduleTimer(System.currentTimeMillis() + 100, "timer1");
        Thread.sleep(200);
        Assert.assertFalse(clientBean.hasTimerRun());
        clientContext.close();

        Collector serverBean = (Collector) getRemoteContext(serverClient).lookup(ARCHIVE_NAME + "/" + CollectionSingleton.class.getSimpleName() + "!" + Collector.class.getName());
        List<TimerData> res = serverBean.collect(1);
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("server", res.get(0).getNode());
        Assert.assertEquals("timer1", res.get(0).getInfo());
    }


    public static Context getRemoteContext(ManagementClient managementClient) throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        URI webUri = managementClient.getWebUri();
        URI namingUri = new URI("remote+http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "", "", "");
        env.put(Context.PROVIDER_URL, namingUri.toString());
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        env.put("jboss.naming.client.security.callback.handler.class", CallbackHandler.class.getName());
        env.put("jboss.naming.client.ejb.context", true);
        return new InitialContext(env);
    }


}

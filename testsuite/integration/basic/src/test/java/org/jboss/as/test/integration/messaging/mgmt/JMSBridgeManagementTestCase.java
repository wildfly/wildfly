/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.messaging.mgmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.SUBSYSTEM;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.STARTED;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ENABLED;
import static org.jboss.as.messaging.MessagingExtension.SUBSYSTEM_NAME;

/**
 * @author baranowb
 * 
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JMSBridgeManagementTestCase extends AbstractMgmtTestBase {

    @ContainerResource
    private ManagementClient managementClient;

    static final PathAddress ADDRESS_HORNETQ = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
            PathElement.pathElement(HORNETQ_SERVER, "default"));
    static final PathAddress ADDRESS_XA_CONNECTION_FACTORY = ADDRESS_HORNETQ.append(PathElement.pathElement(CONNECTION_FACTORY,
            "XAConnectionFactory"));
    static final PathAddress ADDRESS_QUEUE_SOURCE = ADDRESS_HORNETQ.append(PathElement.pathElement(JMS_QUEUE, "sourceQueue"));
    static final PathAddress ADDRESS_QUEUE_TARGET = ADDRESS_HORNETQ.append(PathElement.pathElement(JMS_QUEUE, "targetQueue"));

    static final PathAddress ADDRESS_JMS_BRIDGE = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
            PathElement.pathElement(JMS_BRIDGE, "outBridge"));

    static final PathAddress ADDRESS_SOCKET_BINDING = PathAddress.pathAddress(PathElement.pathElement("socket-binding-group","standard-sockets"),
            PathElement.pathElement("socket-binding","for-netty"));
    
    static final PathAddress ADDRESS_NETTY_CONNECTOR = ADDRESS_HORNETQ.append(PathElement.pathElement("remote-connector", "netty"));
    static final PathAddress ADDRESS_NETTY_ACCEPTOR = ADDRESS_HORNETQ.append(PathElement.pathElement("remote-acceptor", "netty"));

    private List<ServerSetupTask> tasks = new ArrayList<ServerSetupTask>(4);
    private List<ServerSetupTask> tasksToClean = new ArrayList<ServerSetupTask>(4);
    
    public JMSBridgeManagementTestCase(){
        tasks.add(new DisableHornetQSecruity());
        tasks.add(new SetupNetty());
        tasks.add(new SetupConnectionFactory());
        tasks.add(new SetupQueuesFactory());
        tasks.add(new SetupJMSBridge());
    }

    @Before
    public void setup() throws Exception{
        for(ServerSetupTask t:this.tasks){
            this.tasksToClean.add(t);
            t.setup(this.managementClient, "fake");
        }
    }

    @After
    public void cleanup(){
        try{
            for(ServerSetupTask sst:this.tasksToClean){
                sst.tearDown(this.managementClient, "fake");
            }
        }catch(Exception e){
            
        }
        this.tasksToClean.clear();
    }

    @Test
    public void testStopStart() throws Exception {
        //actually even checking state seems enough, but lets try a circle.
        reload(this.managementClient);
        final ModelControllerClient client = this.managementClient.getControllerClient();
        ModelNode operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(ADDRESS_JMS_BRIDGE));
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(NAME).set(STARTED);
        ModelNode result = client.execute(operation);
        Assert.assertTrue("Bridge should start!",result.get(RESULT).asBoolean());
        
        operation = Util.createOperation(STOP, PathAddress.pathAddress(ADDRESS_JMS_BRIDGE));
        result = client.execute(operation);
        operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(ADDRESS_JMS_BRIDGE));
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(NAME).set(STARTED);
        result = client.execute(operation);
        Assert.assertFalse("Bridge should stop!",result.get(RESULT).asBoolean());

        operation = Util.createOperation(START, PathAddress.pathAddress(ADDRESS_JMS_BRIDGE));
        result = client.execute(operation);
        operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(ADDRESS_JMS_BRIDGE));
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(NAME).set(STARTED);
        result = client.execute(operation);
        Assert.assertTrue("Bridge should start!",result.get(RESULT).asBoolean());
    }

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return this.managementClient.getControllerClient();
    }
    public static void reload(final ManagementClient managementClient) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        managementClient.getControllerClient().execute(operation);
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(5000);
                if (managementClient.isServerInRunningState())
                    reloaded = true;
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 10)
                    throw new Exception("Server reloading failed");
            }
        }
    }
    
    
    static class SetupConnectionFactory implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode operation = Util.createAddOperation(ADDRESS_XA_CONNECTION_FACTORY);
            operation.get("factory-type").set("XA_GENERIC");
            operation.get("connector", "netty").set("netty");
            operation.get("entries").add("java:jboss/jms/XAConnectionFactory")
                    .add("java:jboss/exported/jms/XAConnectionFactory");

            ModelNode result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            managementClient.getControllerClient().execute(Util.createRemoveOperation(ADDRESS_XA_CONNECTION_FACTORY));
        }
    }

    static class SetupQueuesFactory implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode result = setUpQueue(managementClient.getControllerClient(), "sourceQueue", ADDRESS_QUEUE_SOURCE);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            result = setUpQueue(managementClient.getControllerClient(), "targetQueue", ADDRESS_QUEUE_TARGET);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            managementClient.getControllerClient().execute(Util.createRemoveOperation(ADDRESS_QUEUE_SOURCE));
            managementClient.getControllerClient().execute(Util.createRemoveOperation(ADDRESS_QUEUE_TARGET));
        }

        private ModelNode setUpQueue(final ModelControllerClient client, final String name, final PathAddress address)
                throws IOException {
            final ModelNode operation = Util.createAddOperation(address);
            operation.get("entries").add("java:jboss/jms/queue/" + name).add("java:jboss/exported/jms/queue/" + name);
            operation.get("durable").set(true);
            return client.execute(operation);

        }
    }

    static class DisableHornetQSecruity implements ServerSetupTask {
        // read/store and in case 'true', change it back to it, since this test does not need security
        private boolean oldValue = true;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(ADDRESS_HORNETQ));
            operation.get(NAME).set(SECURITY_ENABLED.getName());
            ModelNode result = client.execute(operation);
            this.oldValue = result.get(RESULT).asBoolean();
            if (this.oldValue) {
                operation = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.pathAddress(ADDRESS_HORNETQ));
                operation.get(NAME).set(SECURITY_ENABLED.getName());
                operation.get(VALUE).set(false);
                result = client.execute(operation);
                Assert.assertTrue("Failed to disable security in HornetQ", Operations.isSuccessfulOutcome(result));
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (this.oldValue) {
                ModelNode operation = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.pathAddress(ADDRESS_HORNETQ));
                operation.get(NAME).set(SECURITY_ENABLED.getName());
                operation.get(VALUE).set(true);
                ModelNode result = managementClient.getControllerClient().execute(operation);
                Assert.assertTrue("Failed to enable security in HornetQ", Operations.isSuccessfulOutcome(result));
            }
        }

    }

    static class SetupJMSBridge implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode operation = Util.createAddOperation(ADDRESS_JMS_BRIDGE);

            // final ModelNode source = operation.get("source");
            operation.get("source-connection-factory").set("java:jboss/jms/XAConnectionFactory");
            operation.get("source-destination").set("java:jboss/jms/queue/sourceQueue");

            // final ModelNode target = operation.get("target");
            operation.get("target-connection-factory").set("jms/XAConnectionFactory");
            operation.get("target-destination").set("/jms/queue/targetQueue");
            operation.get("target-context", "java.naming.factory.initial").set(
                    "org.jboss.naming.remote.client.InitialContextFactory");
            operation.get("target-context", "java.naming.provider.url").set("http-remoting://localhost:8080 ");

            // other conf opts if need be
            operation.get("quality-of-service").set("ONCE_AND_ONLY_ONCE");
            operation.get("failure-retry-interval").set(60);
            operation.get("max-retries").set("-1");
            operation.get("max-batch-size").set("10");
            operation.get("max-batch-time").set("500");
            operation.get("add-messageID-in-header").set("true");

            final ModelNode result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            managementClient.getControllerClient().execute(Util.createRemoveOperation(ADDRESS_JMS_BRIDGE));
        }
    }

    static class SetupNetty implements ServerSetupTask{

        @Override
        public void setup(ManagementClient managementClient, String arg1) throws Exception {
            ModelNode operation = Util.createAddOperation(ADDRESS_SOCKET_BINDING);
            operation.get(HOST).set(Utils.getHost(managementClient));
            operation.get(PORT).set(5678);
            operation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            
            ModelNode result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            


            operation = Util.createAddOperation(ADDRESS_NETTY_CONNECTOR);
            operation.get("socket-binding").set("for-netty");
           
            result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            
            
            operation = Util.createAddOperation(ADDRESS_NETTY_ACCEPTOR);
            operation.get("socket-binding").set("for-netty");
           
            result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            reload(managementClient);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String arg1) throws Exception {
            
            ModelNode operation = Util.createRemoveOperation(ADDRESS_NETTY_CONNECTOR);
            managementClient.getControllerClient().execute(operation);

            operation = Util.createRemoveOperation(ADDRESS_NETTY_ACCEPTOR);
            managementClient.getControllerClient().execute(operation);

            operation = Util.createRemoveOperation(ADDRESS_SOCKET_BINDING);
            managementClient.getControllerClient().execute(operation);
        }
        
    }
}

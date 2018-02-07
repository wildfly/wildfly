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
package org.jboss.as.test.manualmode.messaging.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTION_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_BRIDGE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.STARTED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SUBSYSTEM;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ENTRIES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SUBSYSTEM_NAME;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 * 
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JMSBridgeManagementTestCase extends AbstractMgmtTestBase {

    private ManagementClient managementClient;
    @ArquillianResource
    private static ContainerController container;

    static final String NAME_QUEUE_SOURCE = "sourceQueue";
    static final String NAME_QUEUE_TARGET = "targetQueue";
    static final String NAME_BRIDGE = "outBridge";
    static final String NAME_CONNECTION_FACTORY = "XAConnectionFactory";
    static final String NAME_CONNECTOR = "netty";
    static final PathAddress ADDRESS_HORNETQ = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
            PathElement.pathElement(SERVER, "default"));
    static final PathAddress ADDRESS_XA_CONNECTION_FACTORY = ADDRESS_HORNETQ.append(PathElement.pathElement(CONNECTION_FACTORY,
            "XAConnectionFactory"));
    static final PathAddress ADDRESS_QUEUE_SOURCE = ADDRESS_HORNETQ.append(PathElement.pathElement(JMS_QUEUE, NAME_QUEUE_SOURCE));
    static final PathAddress ADDRESS_QUEUE_TARGET = ADDRESS_HORNETQ.append(PathElement.pathElement(JMS_QUEUE, NAME_QUEUE_TARGET));

    static final PathAddress ADDRESS_JMS_BRIDGE = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
            PathElement.pathElement(JMS_BRIDGE,NAME_BRIDGE));

    static final PathAddress ADDRESS_SOCKET_BINDING = PathAddress.pathAddress(PathElement.pathElement("socket-binding-group","standard-sockets"),
            PathElement.pathElement(SOCKET_BINDING.getName(),"for-netty"));
    
    static final PathAddress ADDRESS_NETTY_CONNECTOR = ADDRESS_HORNETQ.append(PathElement.pathElement(REMOTE_CONNECTOR, NAME_CONNECTOR));
    static final PathAddress ADDRESS_NETTY_ACCEPTOR = ADDRESS_HORNETQ.append(PathElement.pathElement(REMOTE_ACCEPTOR, NAME_CONNECTOR));

    public static final String CONTAINER = "jbossas-messaging-ha-server1";
    public static final String DEPLOYMENT_NAME = "DUMMY";
    
    private List<ServerSetupTask> tasks = new ArrayList<ServerSetupTask>(4);
    private List<ServerSetupTask> tasksToClean = new ArrayList<ServerSetupTask>(4);
    
    public JMSBridgeManagementTestCase(){
        
        tasks.add(new SetupNetty());
        tasks.add(new SetupConnectionFactory());
        tasks.add(new SetupQueuesFactory());
        tasks.add(new SetupJMSBridge());
        tasks.add(new DisableHornetQSecruity());
    }

    @Before
    public void setup() throws Exception{
        this.container.start(CONTAINER);
        Assert.assertTrue(this.container.isStarted(CONTAINER));
        this.managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(), TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(),CONTAINER);
        for(ServerSetupTask t:this.tasks){
            this.tasksToClean.add(t);
            t.setup(this.managementClient, CONTAINER);
        }
    }

    @After
    public void cleanup(){
        try{
            for(ServerSetupTask sst:this.tasksToClean){
                sst.tearDown(this.managementClient, CONTAINER);
            }
        }catch(Exception e){
            
        }
        this.tasksToClean.clear();
        try{
            reload();
        }catch(Exception e){
            
        }
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addPackage(JMSBridgeManagementTestCase.class.getPackage());
        return ja;
    }
    
    @Test
    public void testStopStart() throws Exception {
        //actually even checking state seems enough, but lets try a circle.
        reload();
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

    public void reload() throws Exception {
        executeReloadAndWaitForCompletion(this.managementClient.getControllerClient(), 100000);
    }
    
    
    static class SetupConnectionFactory implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            final ModelNode attributes = new ModelNode();
            attributes.get(ConnectionFactoryAttributes.Regular.FACTORY_TYPE.getName()).set("XA_GENERIC");
            attributes.get(CommonAttributes.CONNECTORS).add(NAME_CONNECTOR);
            attributes.get(ENTRIES).add("java:jboss/jms/XAConnectionFactory");
            jmsOperations.addJmsConnectionFactory(NAME_CONNECTION_FACTORY, "java:jboss/exported/jms/XAConnectionFactory", attributes);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.removeJmsConnectionFactory(NAME_CONNECTION_FACTORY);
        }
    }

    class SetupQueuesFactory implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            setUpQueue(managementClient.getControllerClient(), NAME_QUEUE_SOURCE, ADDRESS_QUEUE_SOURCE);
            setUpQueue(managementClient.getControllerClient(), NAME_QUEUE_TARGET, ADDRESS_QUEUE_TARGET);

        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.removeJmsQueue(NAME_QUEUE_SOURCE);
            jmsOperations.removeJmsQueue(NAME_QUEUE_TARGET);
        }

        private void setUpQueue(final ModelControllerClient client, final String name, final PathAddress address)
                throws IOException {
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            final ModelNode attributes = new ModelNode();
            attributes.get(ENTRIES).add("java:jboss/jms/queue/" + name);
            attributes.get(DURABLE.getName()).set(true);
            jmsOperations.createJmsQueue(name, "java:jboss/exported/jms/queue/" + name, attributes);
        }
    }

    class DisableHornetQSecruity implements ServerSetupTask {
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

    class SetupJMSBridge implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            final ModelNode attributes = new ModelNode();
            // final ModelNode source = operation.get("source");
            attributes.get(JMSBridgeDefinition.SOURCE_CONNECTION_FACTORY.getName()).set("java:jboss/jms/XAConnectionFactory");
            attributes.get(JMSBridgeDefinition.SOURCE_DESTINATION.getName()).set("java:jboss/jms/queue/sourceQueue");

            // final ModelNode target = operation.get("target");
            attributes.get(JMSBridgeDefinition.TARGET_CONNECTION_FACTORY.getName()).set("jms/XAConnectionFactory");
            attributes.get(JMSBridgeDefinition.TARGET_DESTINATION.getName()).set("/jms/queue/targetQueue");
            
            attributes.get(JMSBridgeDefinition.TARGET_CONTEXT.getName(), "java.naming.factory.initial").set(
                    "org.jboss.naming.remote.client.InitialContextFactory");
            attributes.get(JMSBridgeDefinition.TARGET_CONTEXT.getName(), "java.naming.provider.url").set("http-remoting://"+managementClient.getMgmtAddress()+":8080 ");

            // other conf opts if need be
            attributes.get(JMSBridgeDefinition.QUALITY_OF_SERVICE.getName()).set("ONCE_AND_ONLY_ONCE");
            attributes.get(JMSBridgeDefinition.FAILURE_RETRY_INTERVAL.getName()).set(60);
            attributes.get(JMSBridgeDefinition.MAX_RETRIES.getName()).set("-1");
            attributes.get(JMSBridgeDefinition.MAX_BATCH_SIZE.getName()).set("10");
            attributes.get(JMSBridgeDefinition.MAX_BATCH_TIME.getName()).set("500");
            attributes.get(JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER.getName()).set("true");
            
            jmsOperations.addJmsBridge(NAME_BRIDGE, attributes);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.removeJmsBridge(NAME_BRIDGE);
        }
    }

    class SetupNetty implements ServerSetupTask{

        @Override
        public void setup(ManagementClient managementClient, String arg1) throws Exception {
            ModelNode operation = Util.createAddOperation(ADDRESS_SOCKET_BINDING);
            operation.get(HOST).set(Utils.getHost(managementClient));
            operation.get(PORT).set(5678);
            operation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            
            ModelNode result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            


            operation = Util.createAddOperation(ADDRESS_NETTY_CONNECTOR);
            operation.get(CommonAttributes.SOCKET_BINDING.getName()).set("for-netty");
           
            result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            
            
            operation = Util.createAddOperation(ADDRESS_NETTY_ACCEPTOR);
            operation.get(CommonAttributes.SOCKET_BINDING.getName()).set("for-netty");
           
            result = managementClient.getControllerClient().execute(operation);
            Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
            reload();

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

/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ServerSocketFactory;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerImpl;
import org.jboss.as.domain.controller.DomainControllerSlave;
import org.jboss.as.domain.controller.DomainModel;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.HostControllerClient;
import org.jboss.as.host.controller.mgmt.DomainControllerOperationHandlerImpl;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.mgmt.ManagementHeaderMessageHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Reproduces a problem I was seeing with the protocol hanging for remote DC->HC requests on the incoming connection from HC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteDomainControllerConnectionTestCase {

    DomainController domainController;
    DomainControllerOperationHandlerImpl domainControllerOperationHandlerImpl;
    PathAddress proxyNodeAddress;
    ProtocolServer server;
    CountDownLatch connectionLatch = new CountDownLatch(1);
    Connection clientConn;
    Connection serverConn;
    RemoteDomainConnectionService service;

    @Before
    public void start() throws Exception {
        ExtensibleConfigurationPersister mockPersister = new ExtensibleConfigurationPersister() {

            @Override
            public void registerSubsystemWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {
            }

            @Override
            public void registerSubsystemDeploymentWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {
            }

            @Override
            public void store(ModelNode model) throws ConfigurationPersistenceException {
            }

            @Override
            public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
            }

            @Override
            public List<ModelNode> load() throws ConfigurationPersistenceException {
                return null;
            }
        };
        ModelNode mockModel = new ModelNode();
        mockModel.get(ModelDescriptionConstants.PROFILE);
        DomainModel dm = DomainModel.Factory.create(mockModel, mockPersister, null);
        domainController = new DomainControllerImpl(Executors.newScheduledThreadPool(20), dm, "test", new NoopFileRepository());
        domainControllerOperationHandlerImpl = new DomainControllerOperationHandlerImpl(domainController, new ServerConnectionHandler());

        //Add an empty profile
        ModelNode add = new ModelNode();
        add.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        add.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.PROFILE, "test")).toModelNode());
        domainController.execute(add);


        final ProtocolServer.Configuration config = new ProtocolServer.Configuration();
        config.setBindAddress(new InetSocketAddress(InetAddress.getByName("localhost"), 0));
        final AtomicInteger increment = new AtomicInteger();
        config.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server-" + increment.incrementAndGet());
            }
        });
        config.setReadExecutor(Executors.newCachedThreadPool());
        config.setSocketFactory(ServerSocketFactory.getDefault());
        config.setBacklog(50);
        config.setConnectionHandler(new ServerConnectionHandler());

        server = new ProtocolServer(config);
        server.start();

    }

    @After
    public void stop() {
        server.stop();
    }

    @Test
    public void testRemoteDomainControllerConnection() throws Exception {
        service = new RemoteDomainConnectionService("Test", InetAddress.getByName("localhost"), server.getBoundAddress().getPort(), new NoopFileRepository());
        TestDomainControllerSlave slave = new TestDomainControllerSlave();
        service.register("slave", slave);
        ModelNode remoteModel = slave.model;
        Assert.assertNotNull(remoteModel);
        Assert.assertTrue(remoteModel.hasDefined(ModelDescriptionConstants.PROFILE));
        Assert.assertTrue(remoteModel.get(ModelDescriptionConstants.PROFILE).hasDefined("test"));

//        //Request the profile description from the host - this will be empty
//        ModelNode desc = service.getProfileOperations("test");
//        Assert.assertNotNull(desc);

        //Execute something hitting the host proxy
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.pathAddress().toModelNode());
        op.get(ModelDescriptionConstants.RECURSIVE).set(true);


        ModelNode result = domainController.execute(op);
        Assert.assertEquals("success", result.require("outcome").asString());
    }

    private class ServerConnectionHandler extends ManagementHeaderMessageHandler implements ConnectionHandler {

        @Override
        public MessageHandler handleConnected(Connection connection) throws IOException {
            return this;
        }

        @Override
        protected MessageHandler getHandlerForId(byte handlerId) {
            return domainControllerOperationHandlerImpl;
        }

    }

    private class TestDomainControllerSlave implements DomainControllerSlave {

        private ModelNode model;
        @Override
        public OperationResult execute(ModelNode operation, ResultHandler handler) {
            ModelNode node = new ModelNode();
            node.get("test").set("hello");
            handler.handleResultFragment(new String[0], node);
            handler.handleResultComplete();
            return new BasicOperationResult();
        }

        @Override
        public ModelNode execute(ModelNode operation) throws CancellationException {
            ModelNode node = new ModelNode();
            node.get("test").set("hello");
            return node;
        }

        @Override
        public ModelNode addClient(HostControllerClient hostControllerClient) {
            return null;
        }

        @Override
        public void removeClient(String id) {
        }

        @Override
        public ModelNode getDomainModel() {
            return null;
        }

        @Override
        public ModelNode getProfileOperations(String profileName) {
            return null;
        }

        @Override
        public void setInitialDomainModel(ModelNode initialModel) {
            this.model = initialModel;
        }

        @Override
        public FileRepository getFileRepository() {
            return null;
        }
    }

    private class NoopFileRepository implements FileRepository{

        @Override
        public File getFile(String relativePath) {
            return null;
        }

        @Override
        public File getConfigurationFile(String relativePath) {
            return null;
        }

        @Override
        public File[] getDeploymentFiles(byte[] deploymentHash) {
            return null;
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            return null;
        }

    }

}

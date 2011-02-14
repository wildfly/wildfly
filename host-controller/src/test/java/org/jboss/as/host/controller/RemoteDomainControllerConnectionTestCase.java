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

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerImpl;
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
        domainController = new DomainControllerImpl(new ExtensibleConfigurationPersister() {

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
        });
        domainControllerOperationHandlerImpl = new DomainControllerOperationHandlerImpl(ModelControllerClient.Type.HOST, domainController, new ServerConnectionHandler());

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
        service = new RemoteDomainConnectionService("Test", InetAddress.getByName("localhost"), server.getBoundAddress().getPort());
        ModelNode remoteModel = service.register(new TestHostController());
        Assert.assertNotNull(remoteModel);
        Assert.assertTrue(remoteModel.hasDefined(ModelDescriptionConstants.HOST));
        Assert.assertTrue(remoteModel.get(ModelDescriptionConstants.HOST).has("Test"));
        Assert.assertTrue(remoteModel.hasDefined(ModelDescriptionConstants.PROFILE));
        Assert.assertTrue(remoteModel.get(ModelDescriptionConstants.PROFILE).hasDefined("test"));

        //Request the profile description from the host - this will be empty
        ModelNode desc = service.getProfileOperations("test");
        Assert.assertNotNull(desc);

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

    private class TestHostController implements HostController {

        @Override
        public Cancellable execute(ModelNode operation, ResultHandler handler) {
            ModelNode node = new ModelNode();
            node.get("test").set("hello");
            handler.handleResultFragment(new String[0], node);
            handler.handleResultComplete(null);
            return Cancellable.NULL;
        }

        @Override
        public ModelNode execute(ModelNode operation) throws CancellationException, OperationFailedException {
            ModelNode node = new ModelNode();
            node.get("test").set("hello");
            return node;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public ServerStatus startServer(String serverName) {
            return null;
        }

        @Override
        public ServerStatus restartServer(String serverName) {
            return null;
        }

        @Override
        public ServerStatus restartServer(String serverName, int gracefulTimeout) {
            return null;
        }

        @Override
        public ServerStatus stopServer(String serverName) {
            return null;
        }

        @Override
        public ServerStatus stopServer(String serverName, int gracefulTimeout) {
            return null;
        }

        @Override
        public void registerRunningServer(String serverName, Connection connection) {
        }

        @Override
        public void unregisterRunningServer(String serverName) {
        }
    }

}

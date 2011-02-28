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
package org.jboss.as.controller.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.remote.ModelControllerOperationHandler;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.mgmt.ManagementHeaderMessageHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteModelControllerSetup implements ConnectionHandler {
    private final ModelController controller;
    private final int port;
    ModelControllerOperationHandler operationHandler;
    private ProtocolServer server;

    public RemoteModelControllerSetup(ModelController controller, int port) {
        this.controller = controller;
        this.port = port;
    }

    public int getPort() {
        return server.getBoundAddress().getPort();
    }

    public void start() throws Exception {
        final ProtocolServer.Configuration config = new ProtocolServer.Configuration();
        config.setBindAddress(new InetSocketAddress(InetAddress.getByName("localhost"), port));
        config.setThreadFactory(Executors.defaultThreadFactory());
        config.setReadExecutor(Executors.newCachedThreadPool());
        config.setSocketFactory(ServerSocketFactory.getDefault());
        config.setBacklog(50);
        config.setConnectionHandler(this);

        server = new ProtocolServer(config);
        server.start();

    }

    public void stop() {
        server.stop();
    }

    public MessageHandler handleConnected(Connection connection) throws IOException {
        return new SetupManagementHeaderMessageHandler(controller);
    }

    static class SetupManagementHeaderMessageHandler extends ManagementHeaderMessageHandler {
        final ModelControllerOperationHandler operationHandler;

        SetupManagementHeaderMessageHandler(ModelController controller){
            this.operationHandler = ModelControllerOperationHandler.Factory.create(controller, this);
        }


        @Override
        protected MessageHandler getHandlerForId(byte handlerId) {
            return operationHandler;
        }
    }

}

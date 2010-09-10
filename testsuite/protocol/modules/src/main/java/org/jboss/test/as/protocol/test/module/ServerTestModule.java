/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.test.as.protocol.test.module;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.model.Domain;
import org.jboss.as.model.Host;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.Standalone;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.server.manager.ServerManagerProtocolUtils;
import org.jboss.as.server.manager.ServerManagerProtocolCommand.Command;
import org.jboss.test.as.protocol.support.process.MockProcessManager;
import org.jboss.test.as.protocol.support.process.MockProcessManager.NewConnectionListener;
import org.jboss.test.as.protocol.support.server.ServerStarter;
import org.jboss.test.as.protocol.support.server.manager.MockDirectServerManagerCommunicationHandler;
import org.jboss.test.as.protocol.support.server.manager.MockDirectServerManagerCommunicationListener;
import org.jboss.test.as.protocol.support.server.manager.MockServerManager;
import org.jboss.test.as.protocol.support.server.manager.MockServerManagerMessageHandler;
import org.jboss.test.as.protocol.support.xml.ConfigParser;
import org.jboss.test.as.protocol.test.base.ServerTest;

/**
 * Tests that the server part works in isolation with
 * the process manager and server manager processes mocked up
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerTestModule extends AbstractProtocolTestModule implements ServerTest {

    @Override
    public void testServerStartStop() throws Exception {
        MockProcessManager pm = MockProcessManager.create(1);
        Standalone cfg = getServer("standard", "server-one");

        MockServerManager serverManager = MockServerManager.create();
        MockServerManagerMessageHandler managerMessageHandler = new MockServerManagerMessageHandler();
        MockDirectServerManagerCommunicationListener managerListener = MockDirectServerManagerCommunicationListener.create(serverManager, InetAddress.getLocalHost(), 0, 10, managerMessageHandler);

        QueuedNewConnectionListener newConnectionListener = new QueuedNewConnectionListener();
        pm.setNewConnectionListener(newConnectionListener);
        ServerStarter.createServer("Server:server-one", pm, managerListener.getSmPort());
        newConnectionListener.assertWaitForConnection("Server:server-one");

        assertReadCommand(managerMessageHandler, ServerManagerProtocolCommand.SERVER_AVAILABLE);

        MockDirectServerManagerCommunicationHandler managerHandler = managerListener.getManagerHandler("Server:server-one");
        Assert.assertNotNull(managerHandler);

        managerHandler.sendMessage(ServerManagerProtocolUtils.createCommandBytes(ServerManagerProtocolCommand.START_SERVER, cfg));
        assertReadCommand(managerMessageHandler, ServerManagerProtocolCommand.SERVER_STARTED);

        managerHandler.sendMessage(ServerManagerProtocolCommand.STOP_SERVER.createCommandBytes(null));
        assertReadCommand(managerMessageHandler, ServerManagerProtocolCommand.SERVER_STOPPED);

        waitForClose(managerHandler, 5000);
    }

    @Override
    public void testServerManagerCrashed() throws Exception {
        MockProcessManager pm = MockProcessManager.create(1);
        Standalone cfg = getServer("standard", "server-one");

        MockServerManager serverManager = MockServerManager.create();
        MockServerManagerMessageHandler managerMessageHandler = new MockServerManagerMessageHandler();
        MockDirectServerManagerCommunicationListener managerListener = MockDirectServerManagerCommunicationListener.create(serverManager, InetAddress.getLocalHost(), 0, 10, managerMessageHandler);

        QueuedNewConnectionListener newConnectionListener = new QueuedNewConnectionListener();
        pm.setNewConnectionListener(newConnectionListener);
        ServerStarter.createServer("Server:server-one", pm, managerListener.getSmPort());
        SocketConnection pmServerconnection = newConnectionListener.assertWaitForConnection("Server:server-one");

        assertReadCommand(managerMessageHandler, ServerManagerProtocolCommand.SERVER_AVAILABLE);

        MockDirectServerManagerCommunicationHandler managerHandler = managerListener.getManagerHandler("Server:server-one");
        Assert.assertNotNull(managerHandler);

        managerHandler.sendMessage(ServerManagerProtocolUtils.createCommandBytes(ServerManagerProtocolCommand.START_SERVER, cfg));
        assertReadCommand(managerMessageHandler, ServerManagerProtocolCommand.SERVER_STARTED);

        managerHandler.shutdown();
        managerListener.shutdown();

        managerListener = MockDirectServerManagerCommunicationListener.create(serverManager, InetAddress.getLocalHost(), 0, 10, managerMessageHandler);

        Assert.assertTrue(pmServerconnection.isOpen());

        managerListener.resetNewConnectionLatch(1);
        reconnectServerToServerManager(pmServerconnection, InetAddress.getLocalHost(), managerListener.getSmPort());
        MockDirectServerManagerCommunicationHandler handler2 = managerListener.getManagerHandler("Server:server-one");
        managerListener.waitForNewConnection();
        Assert.assertNotSame(managerHandler, handler2);


        //waitForClose(handler, 5000);
    }

    public void reconnectServerToServerManager(SocketConnection pmConnection, InetAddress addr, int port) throws IOException {
        synchronized (this) {
            OutputStream output = pmConnection.getOutputStream();
            StringBuilder sb = new StringBuilder();
            sb.append(org.jboss.as.process.Command.RECONNECT_SERVER_MANAGER);
            sb.append('\0');
            sb.append(addr.getHostName());
            sb.append('\0');
            sb.append(port);
            sb.append('\n');
            StreamUtils.writeString(output, sb.toString());
            output.flush();
        }
    }


    private ServerManagerProtocolCommand.Command assertReadCommand(MockServerManagerMessageHandler managerMessageHandler, ServerManagerProtocolCommand expectedCommand) throws Exception {
        byte[] sent = managerMessageHandler.awaitAndReadMessage();
        Command cmd = ServerManagerProtocolCommand.readCommand(sent);
        Assert.assertEquals(expectedCommand, cmd.getCommand());
        return cmd;
    }

    private void waitForClose(MockDirectServerManagerCommunicationHandler serverHandler, int timeoutMs) throws InterruptedException {
        // Wait for close
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (serverHandler.isClosed())
                break;
            Thread.sleep(100);
        }
        Assert.assertTrue(serverHandler.isClosed());
    }

    private Standalone getServer(String cfgDir, String serverName) throws Exception {
        File file = new File(findDomainConfigsDir(cfgDir));
        Domain domain = ConfigParser.parseDomain(file);
        Host host = ConfigParser.parseHost(file);
        ServerElement el = host.getServer(serverName);
        Assert.assertNotNull(el);
        return new Standalone(domain, host, serverName);
    }

    private static class QueuedNewConnectionListener implements NewConnectionListener {
        final BlockingQueue<ConnectionData> queue = new LinkedBlockingQueue<ConnectionData>();

        @Override
        public void acceptedConnection(String processName, SocketConnection conn) {
            synchronized (this) {
                queue.add(new ConnectionData(processName, conn));
            }
        }

        public SocketConnection assertWaitForConnection(String expectedName) throws InterruptedException {
            ConnectionData data = queue.poll(10, TimeUnit.SECONDS);
            Assert.assertNotNull(data);
            Assert.assertEquals(expectedName, data.getProcessName());
            return data.getConn();
        }
    }

    private static class ConnectionData{
        String processName;
        SocketConnection conn;

        public ConnectionData(String processName, SocketConnection conn) {
            this.processName = processName;
            this.conn = conn;
        }

        public String getProcessName() {
            return processName;
        }

        public SocketConnection getConn() {
            return conn;
        }
    }
}

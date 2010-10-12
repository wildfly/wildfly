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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.ServerFactory;
import org.jboss.as.model.ServerModel;
import org.jboss.as.process.ProcessManagerMaster;
import org.jboss.as.server.ServerManagerProtocolUtils;
import org.jboss.as.server.ServerState;
import org.jboss.as.server.ServerManagerProtocol.Command;
import org.jboss.as.server.ServerManagerProtocol.ServerManagerToServerProtocolCommand;
import org.jboss.as.server.ServerManagerProtocol.ServerToServerManagerProtocolCommand;
import org.jboss.test.as.protocol.support.process.TestProcessHandler;
import org.jboss.test.as.protocol.support.process.TestProcessHandlerFactory;
import org.jboss.test.as.protocol.support.process.TestProcessManager;
import org.jboss.test.as.protocol.support.process.TestProcessManager.NewConnectionListener;
import org.jboss.test.as.protocol.support.server.TestServerProcess;
import org.jboss.test.as.protocol.support.server.manager.MockServerManagerProcess;
import org.jboss.test.as.protocol.support.server.manager.TestServerManagerMessageHandler.ServerMessage;
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
        setDomainConfigDir("standard");
        TestProcessHandlerFactory processHandlerFactory = new TestProcessHandlerFactory(false, true);
        QueuedNewConnectionListener newConnectionListener = new QueuedNewConnectionListener();
        final TestProcessManager pm = TestProcessManager.create(processHandlerFactory, InetAddress.getLocalHost(), 0, newConnectionListener);

        newConnectionListener.assertWaitForConnection("ServerManager");

        MockServerManagerProcess sm = assertGetServerManager(processHandlerFactory);

        sm.addServerToPm("Server:server-one", pm.getPort());
        sm.startServerInPm("Server:server-one");

        newConnectionListener.assertWaitForConnection("Server:server-one");

        assertReadServerCommand(sm, "Server:server-one", ServerToServerManagerProtocolCommand.SERVER_AVAILABLE);
        ServerModel cfg = getServer("standard", "server-one");
        sm.sendMessageToServer("Server:server-one", ServerManagerToServerProtocolCommand.START_SERVER, cfg);
        assertReadServerCommand(sm, "Server:server-one", ServerToServerManagerProtocolCommand.SERVER_STARTED);
        sm.sendMessageToServer("Server:server-one", ServerManagerToServerProtocolCommand.STOP_SERVER);
        assertReadServerCommand(sm, "Server:server-one", ServerToServerManagerProtocolCommand.SERVER_STOPPED);

        TestServerProcess proc1 = processHandlerFactory.getProcessHandler("Server:server-one").getTestServerProcess();
        proc1.awaitShutdown();

        new Thread(new Runnable() {
            public void run() {
                pm.shutdown();
            }
        }).start();

        sm.waitForShutdownCommand();
        sm.stop();
    }

    @Override
    public void testServersReconnectToRestartedServerManager() throws Exception {
        setDomainConfigDir("standard");
        TestProcessHandlerFactory processHandlerFactory = new TestProcessHandlerFactory(false, true);
        QueuedNewConnectionListener newConnectionListener = new QueuedNewConnectionListener();
        final TestProcessManager pm = TestProcessManager.create(processHandlerFactory, InetAddress.getLocalHost(), 0, newConnectionListener);

        newConnectionListener.assertWaitForConnection("ServerManager");

        MockServerManagerProcess sm = assertGetServerManager(processHandlerFactory);

        sm.addServerToPm("Server:server-one", pm.getPort());
        sm.startServerInPm("Server:server-one");

        newConnectionListener.assertWaitForConnection("Server:server-one");

        assertReadServerCommand(sm, "Server:server-one", ServerToServerManagerProtocolCommand.SERVER_AVAILABLE);
        ServerModel cfg = getServer("standard", "server-one");
        sm.sendMessageToServer("Server:server-one", ServerManagerToServerProtocolCommand.START_SERVER, cfg);
        assertReadServerCommand(sm, "Server:server-one", ServerToServerManagerProtocolCommand.SERVER_STARTED);

        sm.crashServerManager(1);
        sm.stop();

        newConnectionListener.assertWaitForConnection("ServerManager");
        sm = assertGetServerManager(processHandlerFactory);

        sm.sendReconnectServersToProcessManager();
        Command<ServerToServerManagerProtocolCommand> cmd = assertReadServerCommand(sm, "Server:server-one", ServerToServerManagerProtocolCommand.SERVER_RECONNECT_STATUS);
        ServerState state = ServerManagerProtocolUtils.unmarshallCommandData(ServerState.class, cmd);
        Assert.assertSame(ServerState.STARTED, state);

        TestServerProcess proc1 = processHandlerFactory.getProcessHandler("Server:server-one").getTestServerProcess();

        new Thread(new Runnable() {
            public void run() {
                pm.shutdown();
            }
        }).start();

        sm.waitForShutdownCommand();
        proc1.awaitShutdown();
        sm.stop();
    }

    private MockServerManagerProcess assertGetServerManager(TestProcessHandlerFactory processHandlerFactory) throws Exception {
        TestProcessHandler handler = processHandlerFactory.getProcessHandler(ProcessManagerMaster.SERVER_MANAGER_PROCESS_NAME);
        Assert.assertNotNull(handler);
        MockServerManagerProcess mgr = handler.getMockServerManager();
        Assert.assertNotNull(mgr);
        return mgr;
    }

    private Command<ServerToServerManagerProtocolCommand> assertReadServerCommand(MockServerManagerProcess serverManager, String serverName, ServerToServerManagerProtocolCommand expectedCommand) throws Exception {
        ServerMessage msg = serverManager.awaitAndReadMessage();
        return assertServerServerMessage(msg, serverName, expectedCommand);
    }

    private Command<ServerToServerManagerProtocolCommand> assertServerServerMessage(ServerMessage msg, String serverName, ServerToServerManagerProtocolCommand expectedCommand) throws Exception {
        Assert.assertEquals(serverName, msg.getSourceProcess());
        byte[] sent = msg.getMessage();
        Command<ServerToServerManagerProtocolCommand> cmd = ServerToServerManagerProtocolCommand.readCommand(sent);
        Assert.assertEquals(expectedCommand, cmd.getCommand());
        return cmd;
    }

    private ServerModel getServer(String cfgDir, String serverName) throws Exception {
        File file = new File(findDomainConfigsDir(cfgDir));
        DomainModel domain = ConfigParser.parseDomain(file);
        HostModel host = ConfigParser.parseHost(file);
        ServerElement el = host.getServer(serverName);
        Assert.assertNotNull(el);
        final List<AbstractServerModelUpdate<?>> list = new ArrayList<AbstractServerModelUpdate<?>>();
        ServerFactory.combine(domain, host, serverName, list);
        final ServerModel model = new ServerModel(serverName, 0);
        for (AbstractServerModelUpdate<?> update : list) {
            model.update(update);
        }
        return model;
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

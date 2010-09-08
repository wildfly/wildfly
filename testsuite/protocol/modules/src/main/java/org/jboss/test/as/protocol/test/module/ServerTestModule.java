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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLInputFactory;

import junit.framework.Assert;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.model.Domain;
import org.jboss.as.model.Host;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.Standalone;
import org.jboss.as.server.AbstractServer;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.server.manager.ServerManagerProtocolUtils;
import org.jboss.as.server.manager.StandardElementReaderRegistrar;
import org.jboss.as.server.manager.ServerManagerProtocolCommand.Command;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.test.as.protocol.support.process.MockProcessManager;
import org.jboss.test.as.protocol.support.process.MockProcessManager.NewConnectionListener;
import org.jboss.test.as.protocol.support.server.ServerStarter;
import org.jboss.test.as.protocol.support.server.manager.MockDirectServerManagerCommunicationHandler;
import org.jboss.test.as.protocol.support.server.manager.MockDirectServerManagerCommunicationListener;
import org.jboss.test.as.protocol.support.server.manager.MockServerManager;
import org.jboss.test.as.protocol.support.server.manager.MockServerManagerMessageHandler;
import org.jboss.test.as.protocol.test.base.ServerTest;

/**
 * Tests that the server part works in isolation with
 * the process manager and server manager processes mocked up
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerTestModule extends AbstractProtocolTestModule implements ServerTest {

    StandardElementReaderRegistrar extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();

    @Override
    public void testServerStartStop() throws Exception {
        MockProcessManager pm = MockProcessManager.create(1);
        Standalone cfg = getServer("standard", "server-one");

        MockServerManager serverManager = MockServerManager.create();
        MockServerManagerMessageHandler managerMessageHandler = new MockServerManagerMessageHandler();
        //ProcessManagerSlave slave = new ProcessManagerSlave("ServerManager", InetAddress.getLocalHost(), pm.getPort(), managerMessageHandler);
        MockDirectServerManagerCommunicationListener managerListener = MockDirectServerManagerCommunicationListener.create(serverManager, InetAddress.getLocalHost(), 0, 10, managerMessageHandler);

        QueuedNewConnectionListener newConnectionListener = new QueuedNewConnectionListener();
        pm.setNewConnectionListener(newConnectionListener);
        AbstractServer server = ServerStarter.createServer("Server:server-one", pm, managerListener.getSmPort());
        ConnectionData connectionData = newConnectionListener.waitForConnection();
        Assert.assertNotNull(connectionData);

        byte[] sent = managerMessageHandler.awaitAndReadMessage();
        Command cmd = ServerManagerProtocolCommand.readCommand(sent);
        Assert.assertEquals(ServerManagerProtocolCommand.SERVER_AVAILABLE, cmd.getCommand());

        MockDirectServerManagerCommunicationHandler handler = managerListener.getManagerHandler("Server:server-one");
        Assert.assertNotNull(handler);

        handler.sendMessage(ServerManagerProtocolUtils.createCommandBytes(ServerManagerProtocolCommand.START_SERVER, cfg));

        sent = managerMessageHandler.awaitAndReadMessage();
        cmd = ServerManagerProtocolCommand.readCommand(sent);
        Assert.assertEquals(ServerManagerProtocolCommand.SERVER_STARTED, cmd.getCommand());

        handler.sendMessage(ServerManagerProtocolCommand.STOP_SERVER.createCommandBytes(null));

        sent = managerMessageHandler.awaitAndReadMessage();
        cmd = ServerManagerProtocolCommand.readCommand(sent);
        Assert.assertEquals(ServerManagerProtocolCommand.SERVER_STOPPED, cmd.getCommand());

        waitForClose(handler, 5000);

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
        Domain domain = parseDomain(cfgDir);
        Host host = parseHost(cfgDir);
        ServerElement el = host.getServer(serverName);
        Assert.assertNotNull(el);
        return new Standalone(domain, host, serverName);
    }

    private Host parseHost(String cfgDir) throws Exception {
        XMLMapper mapper = XMLMapper.Factory.create();
        extensionRegistrar.registerStandardHostReaders(mapper);
        return parseXml(cfgDir, "host.xml", mapper, Host.class);
    }

    private Domain parseDomain(String cfgDir) throws Exception {
        XMLMapper mapper = XMLMapper.Factory.create();
        extensionRegistrar.registerStandardDomainReaders(mapper);
        return parseXml(cfgDir, "domain.xml", mapper, Domain.class);
    }

    private <T> T parseXml(String cfgDir, String name, XMLMapper mapper, Class<T> type) throws Exception {
        File file = new File(findDomainConfigsDir(cfgDir), name);
        if (!file.exists())
            throw new IllegalStateException("File " + file.getAbsolutePath() + " does not exist.");

        try {
            ParseResult<T> parseResult = new ParseResult<T>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(file))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of " + name, e);
        }
    }

    private static class QueuedNewConnectionListener implements NewConnectionListener {
        final BlockingQueue<ConnectionData> queue = new LinkedBlockingQueue<ConnectionData>();

        @Override
        public void acceptedConnection(String processName, SocketConnection conn) {
            synchronized (this) {
                queue.add(new ConnectionData(processName, conn));
            }
        }

        public ConnectionData waitForConnection() throws InterruptedException {
            return queue.poll(10, TimeUnit.SECONDS);
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

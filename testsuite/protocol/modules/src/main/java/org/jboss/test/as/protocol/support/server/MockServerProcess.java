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
package org.jboss.test.as.protocol.support.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ProcessOutputStreamHandler.Managed;
import org.jboss.as.server.DirectServerSideCommunicationHandler;
import org.jboss.as.server.ServerManagerProtocolUtils;
import org.jboss.as.server.ServerState;
import org.jboss.as.server.ServerManagerProtocol.ServerToServerManagerProtocolCommand;

/**
 * A mock server instance that can be used to verify the commands received from
 * PM and SM and to send commands back to those
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MockServerProcess {

    private final Managed managed;
    private final TestServerSideMessageHandler messageHandler;
    private volatile ProcessManagerServerCommunicationHandler pmHandler;
    private volatile DirectServerSideCommunicationHandler smHandler;
    private volatile InetAddress pmAddress;
    private volatile int pmPort;
    private volatile InetAddress smAddress;
    private volatile int smPort;

    volatile CountDownLatch startLatch = new CountDownLatch(1);

    private MockServerProcess(Managed managed, String processName, List<String> command, InputStream stdin) throws IOException {
        this.managed = managed;
        this.messageHandler = new TestServerSideMessageHandler();
        for (int i = 0 ; i < command.size() ; i++) {
            String cmd = command.get(i);
            if (cmd.equals(CommandLineConstants.INTERPROCESS_PM_ADDRESS)) {
                pmAddress = InetAddress.getByName(command.get(++i));
            } else if (cmd.equals(CommandLineConstants.INTERPROCESS_PM_PORT)) {
                pmPort = Integer.valueOf(command.get(++i));
            } else if (cmd.equals(CommandLineConstants.INTERPROCESS_SM_ADDRESS)) {
                smAddress = InetAddress.getByName(command.get(++i));
            } else if (cmd.equals(CommandLineConstants.INTERPROCESS_SM_PORT)) {
                smPort = Integer.valueOf(command.get(++i));
            }
        }
    }

    public static MockServerProcess create(Managed managed, String processName, List<String> command, InputStream stdin) throws IOException {
        MockServerProcess proc = new MockServerProcess(managed, processName, command, stdin);
        //Need the handler
        proc.pmHandler = ProcessManagerServerCommunicationHandler.create(processName, proc.pmAddress, proc.pmPort, proc.messageHandler);
        proc.smHandler = DirectServerSideCommunicationHandler.create(processName, proc.smAddress, proc.smPort, proc.messageHandler);

        return proc;
    }

    public void sendMessageToManager(ServerToServerManagerProtocolCommand command) throws IOException {
        sendMessageToManager(command, null);
    }

    public void sendMessageToManager(ServerToServerManagerProtocolCommand command, Object data) throws IOException {
        byte[] bytes = ServerManagerProtocolUtils.createCommandBytes(command, data);
        smHandler.sendMessage(bytes);
    }

    public byte[] awaitAndReadMessage() {
        return messageHandler.awaitAndReadMessage();
    }

    public byte[] awaitAndReadMessage(int timeoutMs) {
        return messageHandler.awaitAndReadMessage(timeoutMs);
    }

    public void waitForShutdownCommand() throws InterruptedException {
        messageHandler.waitForShutdownCommand();
    }

    public InetAddress getPmAddress() {
        return pmAddress;
    }

    public int getPmPort() {
        return pmPort;
    }

    public InetAddress getSmAddress() {
        return smAddress;
    }

    public int getSmPort() {
        return smPort;
    }

    public void crashServer(int exitCode) {
        managed.processEnded(exitCode);
        smHandler.shutdown();
        pmHandler.shutdown();
    }

    public String waitForReconnectServer() throws InterruptedException {
        return messageHandler.waitForReconnectServer();
    }

    public String waitForReconnectServer(long timeoutMs) throws InterruptedException {
        return messageHandler.waitForReconnectServer(timeoutMs);
    }

    public void closeProcessManagerConnection() {
        pmHandler.shutdown();
    }

    public void closeServerManagerConnection() {
        smHandler.shutdown();
    }

    public void reconnnectToServerManagerAndSendReconnectStatus(InetAddress smAddress, int smPort, ServerState state) throws IOException {
        this.smAddress = smAddress;
        this.smPort = smPort;
        smHandler = DirectServerSideCommunicationHandler.create(managed.getProcessName(), smAddress, smPort, messageHandler);
        smHandler.sendMessage(ServerManagerProtocolUtils.createCommandBytes(ServerToServerManagerProtocolCommand.SERVER_RECONNECT_STATUS, state));
    }
}

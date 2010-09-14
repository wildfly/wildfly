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
package org.jboss.test.as.protocol.support.process;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.process.Command;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ProcessOutputStreamHandler;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.ProcessOutputStreamHandler.Managed;
import org.jboss.as.process.ProcessOutputStreamHandler.Master;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.server.manager.ServerManagerProtocolUtils;
import org.jboss.as.server.manager.ServerState;
import org.jboss.test.as.protocol.support.server.MockDirectServerSideCommunicationHandler;
import org.jboss.test.as.protocol.support.server.MockServerSideMessageHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MockManagedProcess implements Managed{

    final Master master;

    final String processName;

    final List<String> command;

    volatile SocketConnection pmConnection;

    volatile CountDownLatch startLatch = new CountDownLatch(1);

    volatile InetAddress smAddress;

    volatile int smPort;

    //Connection to ServerManager
    volatile MockDirectServerSideCommunicationHandler handler;
    volatile MockServerSideMessageHandler messageHandler;

    volatile boolean start;


    public MockManagedProcess(Master master, String name, List<String> command) {
        this.master = master;
        this.processName = name;
        this.command = command;
    }

    public void setPmConnection(SocketConnection pmConnection) {
        synchronized (this) {
            this.pmConnection = pmConnection;

            final ProcessOutputStreamHandler outputStreamHandler = new ProcessOutputStreamHandler(master, this, new BufferedInputStream(pmConnection.getInputStream()));
            final Thread outputThread = new Thread(outputStreamHandler);
            outputThread.setName("Process " + processName + " socket reader thread");
            outputThread.start();
        }
    }

    public SocketConnection getPmConnection() {
        return pmConnection;
    }

    public InetAddress getSmAddress() {
        return smAddress;
    }

    public int getSmPort() {
        return smPort;
    }

    @Override
    public void closeCommandStream() {
    }

    @Override
    public String getProcessName() {
        return processName;
    }

    private void waitForLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new RuntimeException("Latch timed out");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void start() {
        synchronized (this) {
            if (start)
                return;
            findSmAddressAndPort();
            if (smAddress != null) {

                //Reuse the same message handler if we were restarted. This is important for the
                //ServerManagerTestModule.testServerFailedXXX tests
                if (messageHandler == null)
                    messageHandler = new MockServerSideMessageHandler();
                handler = MockDirectServerSideCommunicationHandler.create(processName, smAddress, smPort, messageHandler);
                try {
                    handler.sendMessage(ServerManagerProtocolCommand.SERVER_AVAILABLE.createCommandBytes(null));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            start = true;
        }
        startLatch.countDown();
    }

    public void stop() throws IOException {
        if (processName.equals("ServerManager")) {
            OutputStream out = pmConnection.getOutputStream();
            StreamUtils.writeString(out, "SHUTDOWN\n");
            out.flush();
        }
        start = false;
    }


    public void waitForStart() {
        waitForLatch(startLatch);
    }

    void findSmAddressAndPort() {
        try {
            if (command != null) {
                for (int i = 0 ; i < command.size() ; i++) {
                    if (command.get(i).equals(CommandLineConstants.INTERPROCESS_SM_ADDRESS))
                        smAddress = InetAddress.getByName(command.get(++i));
                    if (command.get(i).equals(CommandLineConstants.INTERPROCESS_SM_PORT))
                        smPort = Integer.valueOf(command.get(++i));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendShutdownServers() throws IOException {
        if (processName.equals("ServerManager")) {
            OutputStream output = pmConnection.getOutputStream();
            StreamUtils.writeString(output, Command.SHUTDOWN_SERVERS + "\n");
            output.flush();
        }
    }

    public void sendDown(String downServerName) throws IOException {
        if (processName.equals("ServerManager")) {
            OutputStream output = pmConnection.getOutputStream();
            StringBuilder sb = new StringBuilder();
            sb.append(Command.DOWN);
            sb.append('\0');
            sb.append(downServerName);
            sb.append('\n');
            StreamUtils.writeString(output, sb.toString());
            output.flush();
        }
    }

    public void reconnnectAndSendReconnectStatus(InetAddress smAddress, int smPort, ServerState state) throws IOException {
        this.smAddress = smAddress;
        this.smPort = smPort;
        handler = MockDirectServerSideCommunicationHandler.create(processName, smAddress, smPort, messageHandler);
        handler.sendMessage(ServerManagerProtocolUtils.createCommandBytes(ServerManagerProtocolCommand.SERVER_RECONNECT_STATUS, state));
    }

    public MockServerSideMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public MockDirectServerSideCommunicationHandler getCommunicationHandler() {
        return handler;
    }

    public void resetStartLatch() {
        startLatch = new CountDownLatch(1);
    }
}

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

package org.jboss.as.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.process.ProcessManagerProtocol.IncomingCommand;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingCommand;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingCommandHandler;
import org.jboss.logging.Logger;

/**
 * Remote-process-side counterpart to a {@link ManagedProcess} that exchanges messages
 * with the process-manager-side ManagedProcess.
 *
 * FIXME reliable transmission support (JBAS-8262)
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerSlave {

    Logger log = Logger.getLogger(ProcessManagerSlave.class);
    private final Handler handler;
    private final InputStream input;
    private final OutputStream output;
    private final SocketConnection socketConnection;
    private final Controller controller = new Controller();

    public ProcessManagerSlave(String processName, InetAddress addr, Integer port, Handler handler) {
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        this.socketConnection = SocketConnection.connect(addr, port, "CONNECTED", processName);
        this.input = socketConnection.getInputStream();
        this.output = socketConnection.getOutputStream();
        this.handler = handler;
    }

    public Runnable getController() {
        return controller;
    }

    public void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) throws IOException {
        IncomingCommand.ADD.sendAddProcess(output, processName, command, env, workingDirectory);
    }

    public void startProcess(final String processName) throws IOException {
        IncomingCommand.START.sendStartProcess(output, processName);
    }

    public void stopProcess(final String processName) throws IOException {
        IncomingCommand.STOP.sendStopProcess(output, processName);
    }

    public void removeProcess(final String processName) throws IOException {
        IncomingCommand.REMOVE.sendRemoveProcess(output, processName);
    }

    public void sendMessage(final String recipient, final byte[] message) throws IOException {
        IncomingCommand.SEND.sendMessage(output, recipient, message);
    }


    public void sendStdin(final String recipient, final byte[] message) throws IOException {
        IncomingCommand.SEND_STDIN.sendStdin(output, recipient, message);
    }

    public void broadcastMessage(final byte[] message) throws IOException {
        IncomingCommand.BROADCAST.broadcastMessage(output, message);
    }

    public void serversShutdown() throws IOException {
        IncomingCommand.SERVERS_SHUTDOWN.sendServersShutdown(output);
    }

    public void reconnectServers(InetAddress addr, int port) throws IOException {
        IncomingCommand.RECONNECT_SERVERS.sendReconnectServers(output, addr, port);
    }

    public void reconnectServer(String serverName, InetAddress addr, int port) throws IOException {
        IncomingCommand.RECONNECT_SERVER.sendReconnectServer(output, serverName, addr, port);
    }

    private final class Controller implements Runnable {

        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        public void run() {
            OutgoingCommandHandlerToMessageHandlerAdapter handler = new OutgoingCommandHandlerToMessageHandlerAdapter();
            final InputStream input = ProcessManagerSlave.this.input;
            final StringBuilder b = new StringBuilder();
            try {
                for (;;) {
                    Status status = StreamUtils.readWord(input, b);
                    if (status == Status.END_OF_STREAM) {
                        // no more input
                        shutdown();
                        break;
                    }
                    try {

                        final OutgoingCommand command = OutgoingCommand.valueOf(b.toString());
                        status = command.handleMessage(input, status, handler, b);
                    } catch (IllegalArgumentException e) {
                        // unknown command...
                    }
                    if (status == Status.MORE) StreamUtils.readToEol(input);
                }
            } catch (IOException e) {
                // exception caught, shut down channel and exit
                shutdown();
            }
        }
    }

    public void shutdown() {
        if (controller.shutdown.getAndSet(true)) {
            return;
        }

        try{
            handler.shutdown();
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
        }
        finally {
            if (socketConnection != null) {
                socketConnection.close();
            }

            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    SystemExiter.exit(0);
                }
            });
            thread.setName("Exit thread");
            thread.start();
        }

    }


    public interface Handler {

        void handleMessage(String sourceProcessName, byte[] message);

        void shutdown();

        void shutdownServers();

        void down(String downProcessName);
    }

    private class OutgoingCommandHandlerToMessageHandlerAdapter implements OutgoingCommandHandler {

        @Override
        public void handleDown(String serverName) {
            handler.down(serverName);
        }

        @Override
        public void handleMessage(String sourceProcess, byte[] message) {
            handler.handleMessage(sourceProcess, message);
        }

        @Override
        public void handleReconnectServerManager(String address, String port) {
            log.warn("Wrong command " + OutgoingCommand.RECONNECT_SERVER_MANAGER + " received");
        }

        @Override
        public void handleShutdown() {
            handler.shutdown();
        }

        @Override
        public void handleShutdownServers() {
            handler.shutdownServers();
        }
    }
}

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

import java.io.InputStream;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import org.jboss.as.process.ProcessManagerProtocol.IncomingPmCommand;
import org.jboss.logging.Logger;

public final class ProcessOutputStreamHandler implements Runnable {

    private final Master master;

    private final Managed managed;

    private final InputStream inputStream;

    private final Logger log;

    public ProcessOutputStreamHandler(Master master, Managed managed, InputStream inputStream) {
        this.master = master;
        this.managed = managed;
        this.inputStream = inputStream;
        this.log = Logger.getLogger(this.getClass().getName() + "-" + managed.getProcessName());
    }

    public void run() {

        // FIXME reliable transmission support (JBAS-8262)
        final StringBuilder b = new StringBuilder();
        final String processName = managed.getProcessName();
        try {
            for (;;) {
                Status status = StreamUtils.readWord(inputStream, b);
                if (status == Status.END_OF_STREAM) {
                    log.info("Received end of stream, shutting down " + processName);
                    managed.closeCommandStream();
                    // no more input
                    return;
                }
                try {
                    final IncomingPmCommand command = IncomingPmCommand.valueOf(b.toString());
                    status = command.handleMessage(inputStream, status, master, processName, b);
                } catch (IllegalArgumentException e) {
                    // unknown command...
                    log.error("Received unknown command: " + b.toString());
                }
                if (status == Status.MORE) StreamUtils.readToEol(inputStream);

            }
        } catch (SocketException e) {
            log.error("Socket closed for " + processName + ", shutting down");
            managed.closeCommandStream();
        } catch (Exception e) {
            // exception caught, shut down channel and exit
            log.error("Output stream handler for process " + processName + " caught an exception; shutting down", e);
            managed.closeCommandStream();

        } finally {
            ManagedProcess.safeClose(inputStream);
        }
    }

    public interface Master {
        void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory);
        void startProcess(final String processName);
        void stopProcess(final String processName);
        void removeProcess(final String processName);
        void sendStdin(final String recipient, final byte[] msg);
        void serversShutdown();
        void downServer(String serverName);
        void reconnectServersToServerManager(String smAddress, String smPort);
        void reconnectProcessToServerManager(String server, String smAddress, String smPort);
        boolean isShutdown();
    }

    public interface Managed {
        String getProcessName();
        void closeCommandStream();
        void processEnded(int exitCode);
    }
}

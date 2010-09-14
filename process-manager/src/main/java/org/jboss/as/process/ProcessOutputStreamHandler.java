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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    final Command command = Command.valueOf(b.toString());
                    OUT: switch (command) {
                        case ADD: {
                            if (status != Status.MORE) {
                                break;
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String name = b.toString();
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String workingDirectory = b.toString();
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String sizeString = b.toString();
                            final int size;
                            try {
                                size = Integer.parseInt(sizeString, 10);
                            } catch (NumberFormatException e) {
                                e.printStackTrace(System.err); // FIXME remove
                                break;
                            }
                            final List<String> execCmd = new ArrayList<String>();
                            for (int i = 0; i < size; i++) {
                                status = StreamUtils.readWord(inputStream, b);
                                if (status != Status.MORE) {
                                    break OUT;
                                }
                                execCmd.add(b.toString());
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String mapSizeString = b.toString();
                            final int mapSize, lastEntry;
                            try {
                                mapSize = Integer.parseInt(mapSizeString, 10);
                                lastEntry = mapSize - 1;
                            } catch (NumberFormatException e) {
                                e.printStackTrace(System.err); // FIXME remove
                                break;
                            }
                            final Map<String, String> env = new HashMap<String, String>();
                            for (int i = 0; i < mapSize; i ++) {
                                status = StreamUtils.readWord(inputStream, b);
                                if (status != Status.MORE) {
                                    break OUT;
                                }
                                final String key = b.toString();
                                status = StreamUtils.readWord(inputStream, b);
                                if (status == Status.MORE || (i == lastEntry && status == Status.END_OF_LINE)) {
                                    env.put(key, b.toString());
                                }
                                else {
                                    break OUT;
                                }
                            }
                            master.addProcess(name, execCmd, env, workingDirectory);
                            break;
                        }
                        case START: {
                            if (status != Status.MORE) {
                                break;
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            final String name = b.toString();
                            master.startProcess(name);
                            break;
                        }
                        case STOP: {
                            if (status != Status.MORE) {
                                break;
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            final String name = b.toString();
                            master.stopProcess(name);
                            break;
                        }
                        case SERVERS_SHUTDOWN : {
                            if (processName.equals(ProcessManagerMaster.SERVER_MANAGER_PROCESS_NAME)) {
                                master.serversShutdown();
                            } else {
                                log.warnf("%s received from wrong process %s", Command.SERVERS_SHUTDOWN, processName);
                            }
                            break;
                        }
                        case REMOVE: {
                            if (status != Status.MORE) {
                                break;
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            final String name = b.toString();
                            master.removeProcess(name);
                            break;
                        }
                        case SEND: {
                            if (status != Status.MORE) {
                                break;
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            final String recipient = b.toString();
                            final List<String> msg = new ArrayList<String>(0);
                            while (status == Status.MORE) {
                                status = StreamUtils.readWord(inputStream, b);
                                msg.add(b.toString());
                            }
                            master.sendMessage(processName, recipient, msg);
                            break;
                        }
                        case SEND_BYTES: {
                            if (status != Status.MORE) {
                                break;
                            }
                            status = StreamUtils.readWord(inputStream, b);
                            if (status == Status.MORE) {
                                final String recipient = b.toString();
                                master.sendMessage(processName, recipient, StreamUtils.readBytesWithLength(inputStream));
                                status = StreamUtils.readStatus(inputStream);
                            }
                            break;
                        }
                        case BROADCAST: {
                            final List<String> msg = new ArrayList<String>(0);
                            while (status == Status.MORE) {
                                status = StreamUtils.readWord(inputStream, b);
                                msg.add(b.toString());
                            }
                            master.broadcastMessage(processName, msg);
                            break;
                        }
                        case BROADCAST_BYTES: {
                            if (status == Status.MORE) {
                                master.broadcastMessage(processName, StreamUtils.readBytesWithLength(inputStream));
                                status = StreamUtils.readStatus(inputStream);
                            }
                            break;
                        }
                        case RECONNECT_SERVERS : {
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String smAddress = b.toString();
                            status = StreamUtils.readWord(inputStream, b);
                            final String smPort = b.toString();
                            master.reconnectServersToServerManager(smAddress, smPort);
                            break;
                        }
                        case RECONNECT_SERVER: {
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String serverName = b.toString();
                            status = StreamUtils.readWord(inputStream, b);
                            if (status != Status.MORE) {
                                break;
                            }
                            final String smAddress = b.toString();
                            status = StreamUtils.readWord(inputStream, b);
                            final String smPort = b.toString();
                            master.reconnectProcessToServerManager(serverName, smAddress, smPort);
                            break;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // unknown command...
                    log.error("Received unknown command: " + b.toString());
                }
                if (status == Status.MORE) StreamUtils.readToEol(inputStream);

            }
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
        void sendMessage(final String sender, final String recipient, final List<String> msg);
        void sendMessage(final String sender, final String recipient, final byte[] msg);
        void broadcastMessage(final String sender, final List<String> msg);
        void broadcastMessage(final String sender, final byte[] msg);
        void serversShutdown();
        void downServer(String serverName);
        void reconnectServersToServerManager(String smAddress, String smPort);
        void reconnectProcessToServerManager(String server, String smAddress, String smPort);
    }

    public interface Managed {
        String getProcessName();
        void closeCommandStream();
    }
}

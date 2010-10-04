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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.process.ProcessOutputStreamHandler.Master;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProcessManagerProtocol {

    private static final Logger log = Logger.getLogger(ProcessManagerProtocol.class);

    /**
     * Commands sent from the processes to PM
     */
    public enum IncomingPmCommand {
        /** Tells the process manager to add a process (SM->PM) */
        ADD {
            @Override
            public void sendAddProcess(final OutputStream output, final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) throws IOException {
                if (processName == null) {
                    throw new IllegalArgumentException("processName is null");
                }
                if (command == null) {
                    throw new IllegalArgumentException("command is null");
                }
                if (env == null) {
                    throw new IllegalArgumentException("env is null");
                }
                if (workingDirectory == null) {
                    throw new IllegalArgumentException("workingDirectory is null");
                }
                final StringBuilder b = new StringBuilder(256);
                b.append(this).append('\0');
                b.append(processName).append('\0');
                b.append(workingDirectory).append('\0');
                b.append(command.size()).append('\0');
                for (String str : command) {
                    b.append(str).append('\0');
                }
                b.append(env.size());
                for (Map.Entry<String, String> entry : env.entrySet()) {
                    final String key = entry.getKey();
                    if (key != null) {
                        b.append('\0').append(key);
                        final String value = entry.getValue();
                        b.append('\0');
                        if (value != null) b.append(value);
                    }
                }
                b.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, b);
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {

                Status status = currentStatus;
                if (status != Status.MORE) {
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String name = b.toString();
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String workingDirectory = b.toString();
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String sizeString = b.toString();
                final int size;
                try {
                    size = Integer.parseInt(sizeString, 10);
                } catch (NumberFormatException e) {
                    e.printStackTrace(System.err); // FIXME remove
                    return status;
                }
                final List<String> execCmd = new ArrayList<String>();
                for (int i = 0; i < size; i++) {
                    status = StreamUtils.readWord(inputStream, b);
                    if (status != Status.MORE) {
                        return status;
                    }
                    execCmd.add(b.toString());
                }
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String mapSizeString = b.toString();
                final int mapSize, lastEntry;
                try {
                    mapSize = Integer.parseInt(mapSizeString, 10);
                    lastEntry = mapSize - 1;
                } catch (NumberFormatException e) {
                    e.printStackTrace(System.err); // FIXME remove
                    return status;
                }
                final Map<String, String> env = new HashMap<String, String>();
                for (int i = 0; i < mapSize; i ++) {
                    status = StreamUtils.readWord(inputStream, b);
                    if (status != Status.MORE) {
                        return status;
                    }
                    final String key = b.toString();
                    status = StreamUtils.readWord(inputStream, b);
                    if (status == Status.MORE || (i == lastEntry && status == Status.END_OF_LINE)) {
                        env.put(key, b.toString());
                    }
                    else {
                        return status;
                    }
                }
                master.addProcess(name, execCmd, env, workingDirectory);
                return status;
            }
        },

        /** Tells the process manager to start a process (SM->PM) */
        START {
            @Override
            public void sendStartProcess(final OutputStream output, final String processName) throws IOException {
                if (processName == null) {
                    throw new IllegalArgumentException("processName is null");
                }
                final StringBuilder b = new StringBuilder();
                b.append(this).append('\0');
                b.append(processName);
                b.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, b);
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status != Status.MORE) {
                    //break;
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                final String name = b.toString();
                master.startProcess(name);
                return status;
            }
        },

        /** Tells the process manager to stop a process (SM->PM) */
        STOP{
            @Override
            public void sendStopProcess(final OutputStream output, final String processName) throws IOException {
                if (processName == null) {
                    throw new IllegalArgumentException("processName is null");
                }
                final StringBuilder b = new StringBuilder();
                b.append(this).append('\0');
                b.append(processName);
                b.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, b);
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status != Status.MORE) {
                    //break;
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                final String name = b.toString();
                master.stopProcess(name);
                return status;
            }
        },

        /** Tells the process manager to remove a process (SM->PM) */
        REMOVE{
            @Override
            public void sendRemoveProcess(final OutputStream output, final String processName) throws IOException {
                if (processName == null) {
                    throw new IllegalArgumentException("processName is null");
                }
                final StringBuilder b = new StringBuilder();
                b.append(this).append('\0');
                b.append(processName);
                b.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, b);
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status != Status.MORE) {
                    //break;
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                final String name = b.toString();
                master.removeProcess(name);
                return status;
            }
        },

        /** All the known servers have been shut down (SM->PM). Response to {@link OugoingCommand#SHUTDOWN_SERVERS} */
        SERVERS_SHUTDOWN {
            @Override
            public void sendServersShutdown(final OutputStream output) throws IOException {
                synchronized (output) {
                    StreamUtils.writeString(output, this + "\n");
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                if (processName.equals(ProcessManagerMaster.SERVER_MANAGER_PROCESS_NAME)) {
                    master.serversShutdown();
                } else {
                    log.warnf("%s received from wrong process %s", IncomingPmCommand.SERVERS_SHUTDOWN, processName);
                }
                return currentStatus;
            }
        },

        /** The SM has been restarted, tell all server processes to reconnect (SM->PM)*/
        RECONNECT_SERVERS {
            @Override
            public void sendReconnectServers(final OutputStream output,InetAddress addr, int port) throws IOException {
                StringBuilder b = new StringBuilder();
                b.append(this);
                b.append('\0');
                b.append(addr.getHostAddress());
                b.append('\0');
                b.append(port);
                b.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, b.toString());
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status != Status.MORE) {
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String smAddress = b.toString();
                status = StreamUtils.readWord(inputStream, b);
                final String smPort = b.toString();
                master.reconnectServersToServerManager(smAddress, smPort);
                return status;
            }
        },

        /** The SM detected a server's connection went down, tell it to reconnect (SM->PM)*/
        RECONNECT_SERVER {
            @Override
            public void sendReconnectServer(final OutputStream output,String recipient, InetAddress addr, int port) throws IOException {
                StringBuilder b = new StringBuilder();
                b.append(this);
                b.append('\0');
                b.append(recipient);
                b.append('\0');
                b.append(addr.getHostAddress());
                b.append('\0');
                b.append(port);
                b.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, b.toString());
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status != Status.MORE) {
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String serverName = b.toString();
                status = StreamUtils.readWord(inputStream, b);
                if (status != Status.MORE) {
                    return status;
                }
                final String smAddress = b.toString();
                status = StreamUtils.readWord(inputStream, b);
                final String smPort = b.toString();
                master.reconnectProcessToServerManager(serverName, smAddress, smPort);
                return status;
            }
        },

        /** Sends data to the process stdin (Process->PM) */
        SEND_STDIN {
            @Override
            public void sendStdin(final OutputStream output, final String recipient, final byte[] message) throws IOException {
                if (recipient == null) {
                    throw new IllegalArgumentException("processName is null");
                }
                final StringBuilder b = new StringBuilder();
                b.append(this).append('\0');
                b.append(recipient).append('\0');
                synchronized (output) {
                    StreamUtils.writeString(output, b.toString());
                    StreamUtils.writeInt(output, message.length);
                    output.write(message, 0, message.length);
                    StreamUtils.writeChar(output, '\n');
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status != Status.MORE) {
                    return status;
                }
                status = StreamUtils.readWord(inputStream, b);
                if (status == Status.MORE) {
                    final String recipient = b.toString();
                    master.sendStdin(recipient, StreamUtils.readBytesWithLength(inputStream));
                    status = StreamUtils.readStatus(inputStream);
                }
                return status;
            }
        };

        /**
         * Tell PM to add a process
         *
         * @param output output stream to PM
         * @param processName the name of the process to add
         * @param command the command to start the process
         * @param env the environment
         * @param workingDirectory the working directory for the process
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #ADD}
         */
        public void sendAddProcess(final OutputStream output, final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell PM to start a process
         *
         * @param output output stream to PM
         * @param processName the name of the process to start
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #START}
         */
        public void sendStartProcess(final OutputStream output, final String processName) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell PM to stop a process
         *
         * @param output output stream to PM
         * @param processName the name of the process to stop
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #STOP}
         */
        public void sendStopProcess(final OutputStream output, final String processName) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell PM to remove a process
         *
         * @param output output stream to PM
         * @param processName the name of the process to remove
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #REMOVE}
         */
        public void sendRemoveProcess(final OutputStream output, final String processName) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell PM to send a message to a process
         *
         * @param output output stream to PM
         * @param recipient the name of the process to send message to
         * @param message the message bytes
         * @param workingDirectory the working directory for the process
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #SEND}
         */
        public void sendMessage(final OutputStream output, final String recipient, final byte[] message) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }


        /**
         * Tell PM to send a message to a process via its stdin
         *
         * @param output output stream to PM
         * @param recipient the name of the process to send message to
         * @param message the message bytes
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #SEND_STDIN}
         */
        public void sendStdin(final OutputStream output, final String recipient, final byte[] message) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell PM to send a message to all processes
         *
         * @param output output stream to PM
         * @param message the message bytes
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #BROADCAST}
         */
        public void broadcastMessage(final OutputStream output, final byte[] message) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * SM tells PM that all the services have been shutdown in response to the
         * {@link OutgoingPmCommand#SHUTDOWN_SERVERS} command
         *
         * @param output output stream to PM
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #SERVERS_SHUTDOWN}
         */
        public void sendServersShutdown(final OutputStream output) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * SM tells PM to tell processes to reconnect following a SM restart
         *
         * @param output output stream to PM
         * @param addr the address of the SM
         * @param port the port of the SM
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #RECONNECT_SERVERS}
         */
        public void sendReconnectServers(final OutputStream output, InetAddress addr, int port) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * SM tells PM to tell a process to reconnect following a lost connection
         *
         * @param output output stream to PM
         * @param
         * @param addr the address of the SM
         * @param port the port of the SM
         * @throws IOException if the command could not be sent to PM
         * @throws IllegalStateException if this is not {@link #RECONNECT_SERVER}
         */
        public void sendReconnectServer(final OutputStream output, String recipient, InetAddress addr, int port) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        public abstract Status handleMessage(final InputStream inputStream, final Status currentStatus, final Master master, final String processName, final StringBuilder b) throws IOException;
    }

    /**
     * Commands sent from PM to the processes
     */
    public enum OutgoingPmCommand {
        /** Shutdown a process (PM->Process) */
        SHUTDOWN {
            @Override
            public void sendStop(final OutputStream output) throws IOException {
                synchronized (output) {
                    StreamUtils.writeString(output, OutgoingPmCommand.SHUTDOWN + "\n");
                    output.flush();
                    try {
                        output.close();
                    } catch (IOException ignore) {
                    }

                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final OutgoingPmCommandHandler handler, final StringBuilder b) throws IOException {
                handler.handleShutdown();
                return currentStatus;
            }
        },

        /** Shutdown all the known servers (PM->SM) */
        SHUTDOWN_SERVERS {
            @Override
            boolean sendShutdownServers(final OutputStream output) throws IOException {
                synchronized (output) {
                    final OutputStream stream = output;
                    StreamUtils.writeString(stream, OutgoingPmCommand.SHUTDOWN_SERVERS + "\n");
                    stream.flush();
                    return true;
                }
            }
            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final OutgoingPmCommandHandler handler, final StringBuilder b) throws IOException {
                handler.handleShutdownServers();
                return currentStatus;
            }
        },

        /** Reconnect to the SM (PM->Process). */
        RECONNECT_SERVER_MANAGER {
            @Override
            void sendReconnectToServerManager (final OutputStream output, String addr, int port) throws IOException {
                StringBuilder sb = new StringBuilder();
                sb.append(OutgoingPmCommand.RECONNECT_SERVER_MANAGER);
                sb.append('\0');
                sb.append(addr);
                sb.append('\0');
                sb.append(port);
                sb.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, sb.toString());
                    output.flush();
                }
            }

            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final OutgoingPmCommandHandler handler, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status == Status.MORE) {
                    status = StreamUtils.readWord(inputStream, b);
                    final String address = b.toString();
                    if (status != Status.MORE) {
                        // else it was end of stream, so only a partial was received
                        return status;
                    }
                    status = StreamUtils.readWord(inputStream, b);
                    final String port = b.toString();
                    handler.handleReconnectServerManager(address, port);
                }
                return status;
            }
        },

        /** Sent by PM if when a Process is determined to be down (PM->SM) */
        DOWN {
            @Override
            void sendDown(final OutputStream output, final String stoppedProcessName) throws IOException {
                StringBuilder sb = new StringBuilder();
                sb.append(OutgoingPmCommand.DOWN);
                sb.append('\0');
                sb.append(stoppedProcessName);
                sb.append('\n');
                synchronized (output) {
                    StreamUtils.writeString(output, sb.toString());
                    output.flush();
                }
            }
            @Override
            public Status handleMessage(final InputStream inputStream, final Status currentStatus, final OutgoingPmCommandHandler handler, final StringBuilder b) throws IOException {
                Status status = currentStatus;
                if (status == Status.MORE) {
                    status = StreamUtils.readWord(inputStream, b);
                    handler.handleDown(b.toString());
                }
                return status;
            }
        };

        /**
         * Tell process to stop
         *
         * @param output output stream to process
         * @throws IOException if the command could not be sent to process
         * @throws IllegalStateException if this is not {@link #SHUTDOWN}
         */
        public void sendStop(final OutputStream output) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell SM to stop all server processes
         *
         * @param output output stream to SM
         * @throws IOException if the command could not be sent to process
         * @throws IllegalStateException if this is not {@link #SHUTDOWN_SERVERS}
         */
        boolean sendShutdownServers(final OutputStream output) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell Server process to reconnect to SM
         *
         * @param output output stream to SM
         * @param addr new SM address
         * @param port new SM port
         * @throws IOException if the command could not be sent to process
         * @throws IllegalStateException if this is not {@link #RECONNECT_SERVER_MANAGER}
         */
        void sendReconnectToServerManager (final OutputStream output, String addr, int port) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Send a message to the process
         *
         * @param output output stream to process
         * @param sender the name of the sending process
         * @param msg the message
         * @throws IOException if the command could not be sent to process
         * @throws IllegalStateException if this is not {@link #MSG}
         */
        void sendMsg(final OutputStream output, final String sender, final byte[] msg) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        /**
         * Tell SM that a Server process is down
         *
         * @param output output stream to SM
         * @param stoppedProcessName the process name of the down process
         * @throws IOException if the command could not be sent to process
         * @throws IllegalStateException if this is not {@link #DOWN}
         */
        void sendDown(final OutputStream output, final String stoppedProcessName) throws IOException {
            throw new IllegalStateException("Illegal operation for " + this);
        }

        public abstract Status handleMessage(final InputStream inputStream, final Status currentStatus, final OutgoingPmCommandHandler handler, final StringBuilder b) throws IOException;
    }

    public interface OutgoingPmCommandHandler{
        void handleShutdown();
        void handleShutdownServers();
        void handleDown(String serverName);
        void handleReconnectServerManager(String address, String port);
    }

    public interface IncomingPmCommandHandler {
        void handleShutdown();
        void handleReconnectServer(String addr, String port);
    }
}

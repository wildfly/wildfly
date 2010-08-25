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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.StreamUtils.CheckedBytes;

/**
 * Remote-process-side counterpart to a {@link ManagedProcess} that exchanges messages
 * with the process-manager-side ManagedProcess.
 * 
 * FIXME reliable transmission support (JBAS-8262)
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerSlave {
    
    private final Handler handler;
    private final InputStream input;
    private final OutputStream output;
    private final Socket socket;
    private final Controller controller = new Controller();

    public ProcessManagerSlave(String processName, InetAddress addr, Integer port, Handler handler) {
        //TODO Duplicate code - ServerCommunicationHandler
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        if (addr == null) {
            throw new IllegalArgumentException("addr is null");
        }
        if (port == null) {
            throw new IllegalArgumentException("port is null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }

        try {
            this.socket = new Socket(addr, port);
            this.input = new BufferedInputStream(socket.getInputStream());
            this.output = new BufferedOutputStream(socket.getOutputStream());
            this.handler = handler;
            
            System.err.printf("%s connected to port %d\n", processName, socket.getLocalPort());
            
            //Send start signal to ProcessManager so it can associate our socket with the correct ManagedProcess
            StringBuilder sb = new StringBuilder(256);
            sb.append("STARTED");
            sb.append('\0');
            sb.append(processName);
            sb.append('\n');
            synchronized (output) {
                StreamUtils.writeString(output, sb.toString());
                output.flush();
            }
        } catch (IOException e) {
            if (this.socket != null) {
                closeSocket();
            }
            throw new RuntimeException(e);
        }
        //Duplicate code - ServerCommunicationHandler - END 
    }
    
    private void closeSocket() {
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Runnable getController() {
        return controller;
    }

    public void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) throws IOException {
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
        b.append(Command.ADD).append('\0');
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

    public void startProcess(final String processName) throws IOException {
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        final StringBuilder b = new StringBuilder();
        b.append(Command.START).append('\0');
        b.append(processName);
        b.append('\n');
        synchronized (output) {
            StreamUtils.writeString(output, b);
            output.flush();
        }
    }

    public void stopProcess(final String processName) throws IOException {
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        final StringBuilder b = new StringBuilder();
        b.append(Command.STOP).append('\0');
        b.append(processName);
        b.append('\n');
        synchronized (output) {
            StreamUtils.writeString(output, b);
            output.flush();
        }
    }

    public void removeProcess(final String processName) throws IOException {
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        final StringBuilder b = new StringBuilder();
        b.append(Command.REMOVE).append('\0');
        b.append(processName);
        b.append('\n');
        synchronized (output) {
            StreamUtils.writeString(output, b);
            output.flush();
        }
    }

    public void sendMessage(final String processName, final List<String> message) throws IOException {
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        final StringBuilder b = new StringBuilder();
        b.append(Command.SEND).append('\0');
        b.append(processName);
        for (String str : message) {
            b.append('\0').append(str);
        }
        b.append('\n');
        synchronized (output) {
            StreamUtils.writeString(output, b);
            output.flush();
        }
    }

    public void sendMessage(final String recipient, final byte[] message, final long checksum) throws IOException {
        if (recipient == null) {
            throw new IllegalArgumentException("processName is null");
        }
        final StringBuilder b = new StringBuilder();
        b.append(Command.SEND_BYTES).append('\0');
        b.append(recipient).append('\0');
        synchronized (output) {
            StreamUtils.writeString(output, b.toString());
            StreamUtils.writeInt(output, message.length);
            output.write(message, 0, message.length);
            StreamUtils.writeLong(output, checksum);
            StreamUtils.writeChar(output, '\n');
            output.flush();
        }
    }

    public void broadcastMessage(final List<String> message) throws IOException {
        final StringBuilder b = new StringBuilder();
        b.append(Command.BROADCAST);
        for (String str : message) {
            b.append('\0').append(str);
        }
        b.append('\n');
        synchronized (output) {
            StreamUtils.writeString(output, b);
            output.flush();
        }
    }

    public void broadcastMessage(final byte[] message, final long checksum) throws IOException {
        final StringBuilder b = new StringBuilder();
        b.append(Command.BROADCAST_BYTES).append('\0');
        synchronized (output) {
            StreamUtils.writeString(output, b.toString());
            StreamUtils.writeInt(output, message.length);
            output.write(message);
            StreamUtils.writeLong(output, checksum);
            StreamUtils.writeChar(output, '\n');
            output.flush();
        }
    }

    private final class Controller implements Runnable {

        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        public void run() {
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
                        final Command command = Command.valueOf(b.toString());
                        switch (command) {
                            case SHUTDOWN: {
                                shutdown();
                                break;
                            }
                            case MSG: {
                                if (status == Status.MORE) {
                                    status = StreamUtils.readWord(input, b);
                                    final String sourceProcess = b.toString();
                                    final List<String> msg = new ArrayList<String>();
                                    while (status == Status.MORE) {
                                        status = StreamUtils.readWord(input, b);
                                        msg.add(b.toString());
                                    }
                                    if (status == Status.END_OF_LINE) {
                                        try {
                                            handler.handleMessage(sourceProcess, msg);
                                        } catch (Throwable t) {
                                            // ignored!
                                        }
                                    }
                                    // else it was end of stream, so only a partial was received
                                }
                                break;
                            }
                            case MSG_BYTES: {
                                if (status == Status.MORE) {
                                    status = StreamUtils.readWord(input, b);
                                    final String sourceProcess = b.toString();
                                    if (status == Status.MORE) {
                                        CheckedBytes cb = StreamUtils.readCheckedBytes(input);
                                        status = cb.getStatus();
                                        if (cb.getChecksum() != cb.getExpectedChecksum()) {
                                            // FIXME deal with invalid checksum
                                        }
                                        else {
                                            try {
                                                handler.handleMessage(sourceProcess, cb.getBytes());
                                            } catch (Throwable t) {
                                                // ignored!
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
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
            if (socket != null) {
                closeSocket();
            }

            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    System.exit(0);
                }
            });
            thread.setName("Exit thread");
            thread.start();
        }
        
    }

    
    public interface Handler {
        
        void handleMessage(String sourceProcessName, byte[] message);
        
        void handleMessage(String sourceProcessName, List<String> message);
        
        void shutdown();
    }
}

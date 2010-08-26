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

package org.jboss.as.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.Command;
import org.jboss.as.process.Status;
import org.jboss.as.process.StreamUtils;
import org.jboss.logging.Logger;

/**
 * TODO: We need to establish a full protocol.
 * 
 * @author John E. Bailey
 */
public class ServerCommunicationHandler {
    private static final Logger logger = Logger.getLogger("org.jboss.as.server");

    private final Handler handler;
    private final InputStream input;
    private final OutputStream output;
    private final Socket socket;
    private final Runnable controller = new Controller();

    public ServerCommunicationHandler(String processName, InetAddress addr, Integer port, final Handler handler){
        //TODO Duplicate code - ProcessManagerSlave
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
            
            logger.infof("%s connected to process manager on port %d", processName, socket.getLocalPort());
            
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
        //Duplicate code - ProcessManagerSlave - END 
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

     public void sendMessage(final List<String> message) throws IOException {
        final StringBuilder b = new StringBuilder();
        b.append("SEND");
        b.append('\0').append("ServerManager");
        for (String s : message) {
            b.append('\0').append(s);
        }
        b.append('\n');
        StreamUtils.writeString(output, b);
        output.flush();
    }

    public void sendMessage(final byte[] message) throws IOException {
        final StringBuilder b = new StringBuilder();
        b.append("SEND_BYTES");
        b.append('\0').append("ServerManager");
        b.append('\0');
        StreamUtils.writeString(output, b.toString());
        StreamUtils.writeInt(output, message.length);
        output.write(message, 0, message.length);
        StreamUtils.writeChar(output, '\n');
        output.flush();
    }

    public Runnable getController() {
        return controller;
    }

    private final class Controller implements Runnable {

        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        public void run() {
            final InputStream input = ServerCommunicationHandler.this.input;
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
                                            handler.handleMessage(msg);
                                        } catch (Throwable t) {
                                            logger.error("Caught exception handling message from " + sourceProcess, t);
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
                                        try {
                                            handler.handleMessage(StreamUtils.readBytesWithLength(input));
                                        } catch (Throwable t) {
                                            logger.error("Caught exception handling message from " + sourceProcess, t);
                                        }
                                        status = StreamUtils.readStatus(input);
                                    }                                    
                                }
                                break;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // unknown command...
                        logger.error("Received unknown command: " + b.toString());
                    }
                    if (status == Status.MORE) StreamUtils.readToEol(input);
                }
            } catch (IOException e) {
                // exception caught, shut down channel and exit
                shutdown();
            }
        }

        private void shutdown() {
            if (shutdown.getAndSet(true)) {
                return;
            }

            try {
                ServerCommunicationHandler.this.handler.shutdown();
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
    }

    public interface Handler {

        void handleMessage(byte[] message);

        void handleMessage(List<String> message);

        void shutdown();
    }
}

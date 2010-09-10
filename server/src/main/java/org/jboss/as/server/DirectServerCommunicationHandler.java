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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.SystemExiter;

/**
 * Communication Handler for direct communication between this Server and the
 * Server Manager
 *
 * @author Kabir Khan
 */
public class DirectServerCommunicationHandler extends ServerCommunicationHandler {

    final Runnable controller = new InputStreamHandler();

    public DirectServerCommunicationHandler(String processName, InetAddress addr, Integer port, final Handler handler){
        super(processName, addr, port, handler);
    }

    public void sendMessage(final List<String> message) throws IOException {
        throw new IllegalArgumentException("String communication is not enabled");
    }

    public void sendMessage(final byte[] message) throws IOException {
        OutputStream output = getOutput();
        StreamUtils.writeInt(output, message.length);
        output.write(message, 0, message.length);
        output.flush();
    }


//    class LoggingOutputStream extends OutputStream{
//        OutputStream delegate;
//
//        public LoggingOutputStream(OutputStream delegate) {
//            this.delegate = delegate;
//        }
//
//        @Override
//        public void write(int b) throws IOException {
//            System.out.println("----> " + b);
//            delegate.write(b);
//        }
//
//        @Override
//        public void flush() throws IOException {
//            delegate.flush();
//        }
//    }


    public Runnable getController() {
        return controller;
    }

    class InputStreamHandler implements Runnable {
        AtomicBoolean shutdown = new AtomicBoolean();

        @Override
        public void run() {
            try {
                while (!shutdown.get()) {
                    byte[] bytes = StreamUtils.readBytesWithLength(input);
                    handler.handleMessage(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                shutdown();
            }
        }

        void shutdown() {
            if (!shutdown.getAndSet(true)) {
                DirectServerCommunicationHandler.this.shutdown();

                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        SystemExiter.exit(0);
                    }
                });
                thread.setName("Exit thread");
                thread.start();
            }
        }
    }

//    private final class Controller implements Runnable {
//
//        private final AtomicBoolean shutdown = new AtomicBoolean(false);
//
//        public void run() {
//            final InputStream input = DirectServerCommunicationHandler.this.input;
//            final StringBuilder b = new StringBuilder();
//            try {
//                while (!shutdown.get()) {
//                    Status status = StreamUtils.readWord(input, b);
//                    if (status == Status.END_OF_STREAM) {
//                        // no more input
//                        shutdown();
//                        break;
//                    }
//                    try {
//                        final Command command = Command.valueOf(b.toString());
//                        switch (command) {
//                            case SHUTDOWN: {
//                                shutdown();
//                                break;
//                            }
//                            case MSG: {
//                                if (status == Status.MORE) {
//                                    status = StreamUtils.readWord(input, b);
//                                    final String sourceProcess = b.toString();
//                                    final List<String> msg = new ArrayList<String>();
//                                    while (status == Status.MORE) {
//                                        status = StreamUtils.readWord(input, b);
//                                        msg.add(b.toString());
//                                    }
//                                    if (status == Status.END_OF_LINE) {
//                                        try {
//                                            handler.handleMessage(msg);
//                                        } catch (Throwable t) {
//                                            logger.error("Caught exception handling message from " + sourceProcess, t);
//                                        }
//                                    }
//                                    // else it was end of stream, so only a partial was received
//                                }
//                                break;
//                            }
//                            case MSG_BYTES: {
//                                if (status == Status.MORE) {
//                                    status = StreamUtils.readWord(input, b);
//                                    final String sourceProcess = b.toString();
//                                    if (status == Status.MORE) {
//                                        try {
//                                            handler.handleMessage(StreamUtils.readBytesWithLength(input));
//                                        } catch (Throwable t) {
//                                            logger.error("Caught exception handling message from " + sourceProcess, t);
//                                        }
//                                        status = StreamUtils.readStatus(input);
//                                    }
//                                }
//                                break;
//                            }
//                        }
//                    } catch (IllegalArgumentException e) {
//                        // unknown command...
//                        logger.error("Received unknown command: " + b.toString());
//                    }
//                    if (status == Status.MORE) StreamUtils.readToEol(input);
//                }
//            } catch (IOException e) {
//                // exception caught, shut down channel and exit
//                shutdown();
//            }
//        }
//
//        private void shutdown() {
//            if (shutdown.getAndSet(true)) {
//                return;
//            }
//
//            try {
//                DirectServerCommunicationHandler.this.handler.shutdown();
//            }
//            catch (Throwable t) {
//                t.printStackTrace(System.err);
//            }
//            finally {
//                closeServerManagerConnection();
//
//                final Thread thread = new Thread(new Runnable() {
//                    public void run() {
//                        System.exit(0);
//                    }
//                });
//                thread.setName("Exit thread");
//                thread.start();
//            }
//        }
//    }
}

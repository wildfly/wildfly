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

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.SystemExiter;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerManagerToServerCommandHandler;

/**
 * Communication Handler for direct communication between this Server and the
 * Server Manager
 *
 * @author Kabir Khan
 */
public class DirectServerSideCommunicationHandler extends ServerCommunicationHandler {

    private final Runnable controller = new InputStreamHandler();

    private final ServerManagerToServerCommandHandler handler;

    private DirectServerSideCommunicationHandler(String processName, InetAddress addr, Integer port, ServerManagerToServerCommandHandler handler){
        super(processName, addr, port);
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        this.handler = handler;
    }

    public static DirectServerSideCommunicationHandler create(String processName, InetAddress addr, Integer port, ServerManagerToServerCommandHandler handler){
        DirectServerSideCommunicationHandler comm = new DirectServerSideCommunicationHandler(processName, addr, port, handler);
        comm.start();
        return comm;
    }

    public void sendMessage(final byte[] message) throws IOException {
        OutputStream output = getOutput();
        StreamUtils.writeInt(output, message.length);
        output.write(message, 0, message.length);
        output.flush();
    }

    @Override
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
                    handler.handleCommand(bytes);
                }
            } catch (EOFException e) {
                logger.debug("EOF received");
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                shutdown();
            }
        }

        void shutdown() {
            if (!shutdown.getAndSet(true)) {
                DirectServerSideCommunicationHandler.this.shutdown();

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
}

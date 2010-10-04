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
import java.io.InputStream;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.Status;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.SystemExiter;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommand;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommandHandler;
import org.jboss.logging.Logger;

/**
 * Communication Handler for communication via this server and the process manager
 *
 * <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ProcessManagerServerCommunicationHandler extends ServerCommunicationHandler {
    private static final Logger logger = Logger.getLogger("org.jboss.as.server");

    private final Controller controller = new Controller();

    private final OutgoingPmCommandHandler handler;

    private ProcessManagerServerCommunicationHandler(String processName, InetAddress addr, Integer port, OutgoingPmCommandHandler handler){
        super(processName, addr, port);
        if (handler == null) {
            throw new IllegalArgumentException("Null handler");
        }
        this.handler = handler;
    }

    public static ProcessManagerServerCommunicationHandler create(String processName, InetAddress addr, Integer port, OutgoingPmCommandHandler handler){
        ProcessManagerServerCommunicationHandler comm = new ProcessManagerServerCommunicationHandler(processName, addr, port, handler);
        comm.start();
        return comm;
    }

    @Override
    public Runnable getController() {
        return controller;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private final class Controller implements Runnable {

        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        public void run() {
            final InputStream input = ProcessManagerServerCommunicationHandler.this.input;
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
                        final OutgoingPmCommand command = OutgoingPmCommand.valueOf(b.toString());
                        status = command.handleMessage(input, status, handler, b);
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
                ProcessManagerServerCommunicationHandler.this.handler.handleShutdown();
            }
            catch (Throwable t) {
                t.printStackTrace(System.err);
            }
            finally {
                shutdown();

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

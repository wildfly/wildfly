/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.logging.syslogserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;

import org.jboss.logging.Logger;
import org.productivity.java.syslog4j.SyslogRuntimeException;
import org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServer;

/**
 * Syslog4j server for TCP protocol implementation.
 *
 * @author Josef Cacek
 */
public class TCPSyslogServer extends TCPNetSyslogServer {

    private static final Logger LOGGER = Logger.getLogger(TCPSyslogServer.class);

    @SuppressWarnings("unchecked")
    public TCPSyslogServer() {
        sockets = Collections.synchronizedSet(sockets);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try {
            LOGGER.debug("Creating Syslog server socket");
            this.serverSocket = createServerSocket();
        } catch (IOException e) {
            LOGGER.error("ServerSocket creation failed.", e);
            throw new SyslogRuntimeException(e);
        }

        while (!this.shutdown) {
            try {
                final Socket socket = this.serverSocket.accept();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Handling Syslog client " + socket.getInetAddress());
                }
                new Thread(new TCPSyslogSocketHandler(this.sockets, this, socket)).start();
            } catch (IOException e) {
                LOGGER.error("IOException occurred.", e);
            }
        }
    }

}

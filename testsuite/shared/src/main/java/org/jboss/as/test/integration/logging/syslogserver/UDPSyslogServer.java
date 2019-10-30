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
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.List;

import org.jboss.logging.Logger;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.SyslogRuntimeException;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.impl.net.udp.UDPNetSyslogServer;

/**
 * UDP syslog server implementation for syslog4j.
 *
 * @author Josef Cacek
 */
public class UDPSyslogServer extends UDPNetSyslogServer {

    private static Logger LOGGER = Logger.getLogger(UDPSyslogServer.class);

    @Override
    public void shutdown() {
        super.shutdown();
        thread = null;
    }

    @Override
    public void run() {
        this.shutdown = false;
        try {
            this.ds = createDatagramSocket();
        } catch (Exception e) {
            LOGGER.error("Creating DatagramSocket failed", e);
            throw new SyslogRuntimeException(e);
        }

        byte[] receiveData = new byte[SyslogConstants.SYSLOG_BUFFER_SIZE];

        while (!this.shutdown) {
            try {
                final DatagramPacket dp = new DatagramPacket(receiveData, receiveData.length);
                this.ds.receive(dp);
                final SyslogServerEventIF event = new Rfc5424SyslogEvent(receiveData, dp.getOffset(), dp.getLength());
                List list = this.syslogServerConfig.getEventHandlers();
                for (int i = 0; i < list.size(); i++) {
                    SyslogServerEventHandlerIF eventHandler = (SyslogServerEventHandlerIF) list.get(i);
                    eventHandler.event(this, event);
                }
            } catch (SocketException se) {
                LOGGER.warn("SocketException occurred", se);
            } catch (IOException ioe) {
                LOGGER.warn("IOException occurred", ioe);
            }
        }
    }
}

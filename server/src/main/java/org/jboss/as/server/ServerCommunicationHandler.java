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
import java.io.OutputStream;
import java.net.InetAddress;

import org.jboss.as.communication.SocketConnection;
import org.jboss.logging.Logger;

/**
 * Base class for the socket connections to the process manager and to the server manager
 *
 * @author John E. Bailey
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ServerCommunicationHandler {
    protected final Logger logger = Logger.getLogger(this.getClass());

    protected final Handler handler;
    protected final InputStream input;
    protected final OutputStream output;
    private final SocketConnection managerConnection;

    public ServerCommunicationHandler(String processName, InetAddress addr, Integer port, final Handler handler){
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }

        this.managerConnection = SocketConnection.connect(addr, port, "CONNECTED", processName);
        this.input = managerConnection.getInputStream();
        this.output = managerConnection.getOutputStream();
        this.handler = handler;
    }

    abstract void sendMessage(final byte[] message) throws IOException;

    abstract Runnable getController();

    protected void start() {
        Thread t = new Thread(getController(), "Server Process");
        t.start();
    }

    public InputStream getInput() {
        return input;
    }

    public OutputStream getOutput() {
        return output;
    }

    public void shutdown() {
        managerConnection.close();
    }

    public boolean isClosed() {
        return !managerConnection.isOpen();
    }

    public interface Handler {

        void handleMessage(byte[] message);

        void shutdown();

        void reconnectServer(String addr, String port);
    }

}

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
package org.jboss.as.communication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.jboss.as.process.StreamUtils;
import org.jboss.logging.Logger;

/**
 * Wraps a socket
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SocketConnection {

    private static final Logger log = Logger.getLogger(SocketConnection.class);
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private boolean closing;

    private SocketConnection(Socket socket) throws IOException{
        this.socket = socket;
        this.input = new BufferedInputStream(socket.getInputStream());
        this.output = new BufferedOutputStream(socket.getOutputStream());
    }

    /**
     * Creates a socket connection on the server side
     */
    public static SocketConnection accepted(Socket socket) {
        if (socket.isClosed())
            throw new IllegalArgumentException("Socket is closed");
        try {
            return new SocketConnection(socket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * creates a socket connection on the client side
     *
     * @param addr the address to connect to
     * @param port the port to connect to
     * @param initialRequestElements the elements of the initial request. They will be merged into a string with '\0' separating the elements and '\n' on the end
     * @return the SocketConnection wrapper
     */
    public static SocketConnection connect(InetAddress addr, Integer port, String...initialRequestElements) {
        if (addr == null) {
            throw new IllegalArgumentException("addr is null");
        }
        if (port == null) {
            throw new IllegalArgumentException("port is null");
        }

        Socket socket = null;
        try {
            log.infof("Trying to connect to %s on port %d", addr, port);
            socket = new Socket(addr, port);
            SocketConnection conn = new SocketConnection(socket);

            String request = createInitalRequest(initialRequestElements);

            log.infof("Connected to port %d on %s via local port %d. Sending initial request: %s", port, addr, socket.getLocalPort(), request.trim());
            if (request.length() > 0)
                conn.sendInitialRequest(request);
            return conn;
        } catch (IOException e) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
            throw new RuntimeException(e);
        }
        //Duplicate code - ServerCommunicationHandler - END
    }

    private static String createInitalRequest(String...initialRequestElements) {
        StringBuilder initialRequest = new StringBuilder();
        for (int i = 0 ; i < initialRequestElements.length ; i++) {
            initialRequest.append(initialRequestElements[i]);
            if (i == initialRequestElements.length - 1)
                initialRequest.append('\n');
            else
                initialRequest.append('\0');
        }
        return initialRequest.toString();
    }

    private void sendInitialRequest(String initialRequest) throws IOException {
        try {
            synchronized (output) {
                StreamUtils.writeString(output, initialRequest);
                output.flush();
            }
        } catch (IOException e) {
                close();
            throw e;
        }
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public synchronized void close() {
        if (closing)
            return;
        closing = true;
        log.infof("Closing connection local port: %d, remote port: %d", socket.getLocalPort(), socket.getPort());
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

    public InputStream getInputStream() {
        return input;
    }

    public OutputStream getOutputStream() {
        return output;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.MessageProp;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;

/**
 * A sample server application for testing Kerberos identity propagation.
 *
 * @author Josef Cacek
 */
public class GSSTestServer implements ServerSetupTask, Runnable {

    private static Logger LOGGER = Logger.getLogger(GSSTestServer.class);

    private static final boolean SKIP_TASK;

    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    private volatile boolean serverStarted = false;

    // Public methods --------------------------------------------------------

    /**
     * Starts instance of this {@link GSSTestServer} in the new Thread.
     *
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     * java.lang.String)
     */
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        // skip server initialization if Kerberos is not able to work correctly.
        // JUnit's AssumptionViolationException is not handled correctly in ServerSetupTask instances
        if (SKIP_TASK) { return; }

        new Thread(this).start();
        int i = 0;
        while (!serverStarted && i < 20) {
            i++;
            try {
                Thread.sleep(ADJUSTED_SECOND);
            } catch (InterruptedException e) {
                LOGGER.trace("Interrupted", e);
            }
        }

        final Socket socket = new Socket();
        try {
            LOGGER.debug("Waiting for the GSSTestServer.");
            socket.connect(new InetSocketAddress(InetAddress.getByName(null), GSSTestConstants.PORT), 20 * ADJUSTED_SECOND);
            LOGGER.debug("GSSTestServer is up");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("Problem occurred during closing socket", e);
            }
        }

    }

    /**
     * Stops instance on this {@link GSSTestServer}.
     *
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     * java.lang.String)
     */
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (SKIP_TASK) { return; }
        stop();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            start();
        } catch (LoginException e) {
            LOGGER.error("LoginException: ", e);
            throw new RuntimeException(e);
        } catch (PrivilegedActionException e) {
            LOGGER.error("PrivilegedActionException: ", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOGGER.error("IOException: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The Main. It sends stop command to a running {@link GSSTestServer} instance when the first argument provided is "stop",
     * otherwise it starts a new server instance.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length > 0 && "stop".equals(args[0])) {
            stop();
        } else {
            final GSSTestServer gssTestServer = new GSSTestServer();
            try {
                gssTestServer.start();
            } catch (Exception e) {
                LOGGER.error("Problem occurred", e);
                System.exit(1);
            }
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Sends STOP ({@link GSSTestConstants#CMD_STOP}) command to a running server.
     */
    private static void stop() {
        LOGGER.debug("Sending STOP command GSSTestServer.");
        // Create an unbound socket
        final Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName(null), GSSTestConstants.PORT),
                    GSSTestConstants.SOCKET_TIMEOUT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(GSSTestConstants.CMD_STOP);
            dos.flush();
            LOGGER.debug("STOP command sent.");
            // wait for the GSSServer cleanup
            Thread.sleep(1000L);
        } catch (IOException e) {
            LOGGER.error("Problem occurred during sending stop command", e);
        } catch (InterruptedException e) {
            LOGGER.trace("Thread.sleep() interrupted", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("Problem occurred during closing socket", e);
            }
        }
    }

    /**
     * Authenticates the server in Kerberos KDC and starts the {@link ServerAction} instance as the authenticated subject.
     *
     * @throws LoginException
     * @throws PrivilegedActionException
     * @throws IOException
     */
    private void start() throws LoginException, PrivilegedActionException, IOException {
        LOGGER.debug("Starting GSSTestServer - login");
        // Use our custom configuration to avoid reliance on external config
        final Krb5LoginConfiguration krb5configuration = new Krb5LoginConfiguration(null, null, true,
                Utils.getLoginConfiguration());
        Configuration.setConfiguration(krb5configuration);
        // 1. Authenticate to Kerberos.
        final LoginContext lc = Utils.loginWithKerberos(krb5configuration, GSSTestConstants.PRINCIPAL,
                GSSTestConstants.PASSWORD);
        LOGGER.debug("Authentication succeed");
        // 2. Perform the work as authenticated Subject.
        final String finishMsg = Subject.doAs(lc.getSubject(), new ServerAction());
        LOGGER.trace("Server stopped with result: " + (finishMsg == null ? "OK" : finishMsg));
        lc.logout();
        krb5configuration.resetConfiguration();
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A ServerAction which creates a ServerSocket and waits for clients. It sends back the authenticated client name.
     *
     * @author Josef Cacek
     */
    private class ServerAction implements PrivilegedAction<String> {

        public String run() {
            final GSSManager gssManager = GSSManager.getInstance();
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(GSSTestConstants.PORT);
                LOGGER.trace("Server started on port " + GSSTestConstants.PORT);
                int command = GSSTestConstants.CMD_NOOP;

                serverStarted = true;

                do {
                    Socket socket = null;
                    GSSContext gssContext = null;
                    try {
                        LOGGER.debug("Waiting for client connection");
                        socket = serverSocket.accept();
                        LOGGER.debug("Client connected");
                        gssContext = gssManager.createContext((GSSCredential) null);
                        final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                        final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                        command = dataInputStream.readInt();
                        LOGGER.debug("Command code: " + command);
                        if (command == GSSTestConstants.CMD_NAME) {
                            while (!gssContext.isEstablished()) {
                                final byte[] inToken = new byte[dataInputStream.readInt()];
                                dataInputStream.readFully(inToken);
                                final byte[] outToken = gssContext.acceptSecContext(inToken, 0, inToken.length);

                                if (outToken != null) {
                                    dataOutputStream.writeInt(outToken.length);
                                    dataOutputStream.write(outToken);
                                    dataOutputStream.flush();
                                }
                            }
                            final String clientName = gssContext.getSrcName().toString();
                            LOGGER.trace("Context Established with Client " + clientName);

                            // encrypt
                            final MessageProp msgProp = new MessageProp(true);
                            final byte[] clientNameBytes = clientName.getBytes(GSSTestConstants.CHAR_ENC);
                            final byte[] outToken = gssContext.wrap(clientNameBytes, 0, clientNameBytes.length, msgProp);

                            dataOutputStream.writeInt(outToken.length);
                            dataOutputStream.write(outToken);
                            dataOutputStream.flush();
                            LOGGER.trace("Client name was returned as the token value.");
                        }
                    } catch (EOFException e) {
                        LOGGER.trace("Client didn't send a correct message.");
                    } catch (IOException e) {
                        LOGGER.error("IOException occurred", e);
                    } catch (GSSException e) {
                        LOGGER.error("GSSException occurred", e);
                    } finally {
                        if (gssContext != null) {
                            try {
                                gssContext.dispose();
                            } catch (GSSException e) {
                                LOGGER.error("Problem occurred during disposing GSS context", e);
                            }
                        }
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                LOGGER.error("Problem occurred during closing a Socket", e);
                            }
                        }

                    }
                } while (command != GSSTestConstants.CMD_STOP);
                LOGGER.trace("Stop command received.");
            } catch (IOException e) {
                LOGGER.error("IOException occurred", e);
                return e.getMessage();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        LOGGER.error("Problem occurred during closing a ServerSocket", e);
                    }
                }
            }
            return null;
        }

    }

    static {
        boolean skipTask = false;
        try {
            KerberosTestUtils.assumeKerberosAuthenticationSupported();
        } catch (Exception e) {
            skipTask = true;
        }
        SKIP_TASK = skipTask;
    }
}

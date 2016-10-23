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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.MessageProp;
import org.jboss.logging.Logger;
import org.jboss.security.negotiation.Constants;

/**
 * A client for {@link GSSTestServer}.
 *
 * @author Josef Cacek
 */
public class GSSTestClient {

    private static Logger LOGGER = Logger.getLogger(GSSTestClient.class);

    private final String host;
    private final int port;
    private final String spn;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new GSSTestClient.
     *
     * @param serverHost
     * @param serverPort
     * @param spn        Service Principal Name
     */
    public GSSTestClient(String serverHost, int serverPort, String spn) {
        this.host = serverHost;
        this.port = serverPort;
        this.spn = spn;
    }

    // Public methods --------------------------------------------------------

    /**
     * Retrieves the name of calling identity (based on given gssCredential) retrieved from {@link GSSTestServer}.
     *
     * @param gssCredential
     * @return
     * @throws IOException
     * @throws GSSException
     */
    public String getName(final GSSCredential gssCredential) throws IOException, GSSException {
        LOGGER.trace("getName() called with GSSCredential:\n" + gssCredential);
        // Create an unbound socket
        final Socket socket = new Socket();
        GSSContext gssContext = null;
        try {
            socket.connect(new InetSocketAddress(host, port), GSSTestConstants.SOCKET_TIMEOUT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            LOGGER.debug("Sending NAME command.");
            dos.writeInt(GSSTestConstants.CMD_NAME);
            dos.flush();

            GSSManager manager = GSSManager.getInstance();
            gssContext = manager.createContext(manager.createName(spn, null), Constants.KERBEROS_V5, gssCredential,
                    GSSContext.DEFAULT_LIFETIME);

            //            gssContext.requestCredDeleg(true);
            gssContext.requestMutualAuth(true);
            gssContext.requestConf(true);
            gssContext.requestInteg(true);

            byte[] token = new byte[0];
            while (!gssContext.isEstablished()) {
                token = gssContext.initSecContext(token, 0, token.length);
                if (token != null) {
                    dos.writeInt(token.length);
                    dos.write(token);
                    dos.flush();
                }
                if (!gssContext.isEstablished()) {
                    token = new byte[dis.readInt()];
                    dis.readFully(token);
                }
            }

            token = new byte[dis.readInt()];
            dis.readFully(token);
            MessageProp msgProp = new MessageProp(false);
            final byte[] nameBytes = gssContext.unwrap(token, 0, token.length, msgProp);
            return new String(nameBytes, GSSTestConstants.CHAR_ENC);
        } catch (IOException e) {
            LOGGER.error("IOException occurred.", e);
            throw e;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("IOException occurred", e);
            }
            if (gssContext != null) {
                try {
                    gssContext.dispose();
                } catch (GSSException e) {
                    LOGGER.error("GSSException occurred", e);
                }
            }
        }
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "GSSTestClient [host=" + host + ", port=" + port + ", spn=" + spn + "]";
    }

}

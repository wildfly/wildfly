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
package org.jboss.as.test.integration.security.common.negotiation;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.CharArrayBuffer;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.jboss.logging.Logger;

/**
 * SPNEGO (Simple and Protected GSSAPI Negotiation Mechanism) authentication scheme. It's based on NegotiateScheme class from
 * Apache HC, it fixes DEFAULT_LIFETIME problem with IBM JDK.
 * <p>
 * This class could extend {@link org.apache.http.impl.auth.SPNegoScheme SPNegoScheme} when the <a
 * href="https://issues.apache.org/jira/browse/HTTPCLIENT-1305">HTTPCLIENT-1305</a> is fixed.
 *
 * @author Josef Cacek
 */
public class JBossNegotiateScheme extends AuthSchemeBase {

    private static final Logger LOGGER = Logger.getLogger(JBossNegotiateScheme.class);

    /** The DEFAULT_LIFETIME */
    private static final int DEFAULT_LIFETIME = 60;
    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    enum State {
        UNINITIATED, CHALLENGE_RECEIVED, TOKEN_GENERATED, FAILED,
    }

    private final boolean stripPort;

    /** Authentication process state */
    private State state;

    /** base64 decoded challenge **/
    private byte[] token;

    private final Base64 base64codec;

    // Constructors ----------------------------------------------------------

    /**
     * Default constructor for the Negotiate authentication scheme.
     */
    public JBossNegotiateScheme(boolean stripPort) {
        super();
        this.state = State.UNINITIATED;
        this.stripPort = stripPort;
        this.base64codec = new Base64(0);
    }

    // Public methods --------------------------------------------------------

    /**
     * Tests if the Negotiate authentication process has been completed.
     *
     * @return <tt>true</tt> if authorization has been processed, <tt>false</tt> otherwise.
     */
    public boolean isComplete() {
        return this.state == State.TOKEN_GENERATED || this.state == State.FAILED;
    }

    /**
     * Returns textual designation of the Negotiate authentication scheme.
     *
     * @return <code>Negotiate</code>
     */
    public String getSchemeName() {
        return "Negotiate";
    }

    @Deprecated
    public Header authenticate(final Credentials credentials, final HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request, null);
    }

    /**
     * Produces Negotiate authorization Header based on token created by processChallenge.
     *
     * @param credentials Never used be the Negotiate scheme but must be provided to satisfy common-httpclient API. Credentials
     *        from JAAS will be used instead.
     * @param request The request being authenticated
     *
     * @throws AuthenticationException if authorization string cannot be generated due to an authentication failure
     *
     * @return an Negotiate authorization Header
     */
    @Override
    public Header authenticate(final Credentials credentials, final HttpRequest request, final HttpContext context)
            throws AuthenticationException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (state == State.TOKEN_GENERATED) {
            // hack for auto redirects
            return new BasicHeader("X-dummy", "Token already generated");
        }
        if (state != State.CHALLENGE_RECEIVED) {
            throw new IllegalStateException("Negotiation authentication process has not been initiated");
        }
        try {
            String key = HttpCoreContext.HTTP_TARGET_HOST;
            HttpHost host = (HttpHost) context.getAttribute(key);
            if (host == null) {
                throw new AuthenticationException("Authentication host is not set " + "in the execution context");
            }
            String authServer;
            if (!this.stripPort && host.getPort() > 0) {
                authServer = host.toHostString();
            } else {
                authServer = host.getHostName();
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("init " + authServer);
            }
            final Oid negotiationOid = new Oid(SPNEGO_OID);

            final GSSManager manager = GSSManager.getInstance();
            final GSSName serverName = manager.createName("HTTP@" + authServer, GSSName.NT_HOSTBASED_SERVICE);
            final GSSContext gssContext = manager.createContext(serverName.canonicalize(negotiationOid), negotiationOid, null,
                    DEFAULT_LIFETIME);
            gssContext.requestMutualAuth(true);
            gssContext.requestCredDeleg(true);

            if (token == null) {
                token = new byte[0];
            }
            token = gssContext.initSecContext(token, 0, token.length);
            if (token == null) {
                state = State.FAILED;
                throw new AuthenticationException("GSS security context initialization failed");
            }

            state = State.TOKEN_GENERATED;
            String tokenstr = new String(base64codec.encode(token));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending response '" + tokenstr + "' back to the auth server");
            }
            CharArrayBuffer buffer = new CharArrayBuffer(32);
            if (isProxy()) {
                buffer.append(AUTH.PROXY_AUTH_RESP);
            } else {
                buffer.append(AUTH.WWW_AUTH_RESP);
            }
            buffer.append(": Negotiate ");
            buffer.append(tokenstr);
            return new BufferedHeader(buffer);
        } catch (GSSException gsse) {
            state = State.FAILED;
            if (gsse.getMajor() == GSSException.DEFECTIVE_CREDENTIAL || gsse.getMajor() == GSSException.CREDENTIALS_EXPIRED)
                throw new InvalidCredentialsException(gsse.getMessage(), gsse);
            if (gsse.getMajor() == GSSException.NO_CRED)
                throw new InvalidCredentialsException(gsse.getMessage(), gsse);
            if (gsse.getMajor() == GSSException.DEFECTIVE_TOKEN || gsse.getMajor() == GSSException.DUPLICATE_TOKEN
                    || gsse.getMajor() == GSSException.OLD_TOKEN)
                throw new AuthenticationException(gsse.getMessage(), gsse);
            // other error
            throw new AuthenticationException(gsse.getMessage());
        }
    }

    /**
     * Returns the authentication parameter with the given name, if available.
     *
     * <p>
     * There are no valid parameters for Negotiate authentication so this method always returns <tt>null</tt>.
     * </p>
     *
     * @param name The name of the parameter to be returned
     *
     * @return the parameter with the given name
     */
    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        return null;
    }

    /**
     * The concept of an authentication realm is not supported by the Negotiate authentication scheme. Always returns
     * <code>null</code>.
     *
     * @return <code>null</code>
     */
    public String getRealm() {
        return null;
    }

    /**
     * Returns <tt>true</tt>. Negotiate authentication scheme is connection based.
     *
     * @return <tt>true</tt>.
     */
    public boolean isConnectionBased() {
        return true;
    }

    // Protected methods -----------------------------------------------------

    @Override
    protected void parseChallenge(final CharArrayBuffer buffer, int beginIndex, int endIndex)
            throws MalformedChallengeException {
        String challenge = buffer.substringTrimmed(beginIndex, endIndex);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received challenge '" + challenge + "' from the auth server");
        }
        if (state == State.UNINITIATED) {
            token = new Base64().decode(challenge.getBytes());
            state = State.CHALLENGE_RECEIVED;
        } else {
            LOGGER.debug("Authentication already attempted");
            state = State.FAILED;
        }
    }

}

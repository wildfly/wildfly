/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.RemotingMessages.MESSAGES;
import static org.xnio.Options.SASL_MECHANISMS;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import static org.xnio.Options.SASL_PROPERTIES;
import static org.xnio.Options.SSL_ENABLED;
import static org.xnio.Options.SSL_STARTTLS;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;
import static org.xnio.SslClientAuthMode.REQUESTED;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Property;
import org.xnio.Sequence;
import org.xnio.Xnio;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

/**
 * A Remoting ServerAuthenticationProvider that wraps a management domain security realm.
 * <p/>
 * In addition to making the CallbackHandler available for the requested mechanism this class
 * will also generate the initial OptionsMap based on the capabilities of the security realm.
 * <p/>
 * Initially this only verified that DIGEST-MD5 can be supported.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmSecurityProvider implements RemotingSecurityProvider {

    static final String REALM_PROPERTY = "com.sun.security.sasl.digest.realm";
    static final String PRE_DIGESTED_PROPERTY = "org.jboss.sasl.digest.pre_digested";
    static final String LOCAL_DEFAULT_USER = "jboss.sasl.local-user.default-user";
    static final String LOCAL_USER_CHALLENGE_PATH = "jboss.sasl.local-user.challenge-path";

    static final String ANONYMOUS = "ANONYMOUS";
    static final String DIGEST_MD5 = "DIGEST-MD5";
    static final String EXTERNAL = "EXTERNAL";
    static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    static final String PLAIN = "PLAIN";

    private static final String DOLLAR_LOCAL = "$local";

    private final SecurityRealm realm;
    private final CallbackHandler serverCallbackHandler;
    private final String tokensDir;

    public RealmSecurityProvider(final SecurityRealm realm, final CallbackHandler serverCallbackHandler, final String tokensDir) {
        this.realm = realm;
        this.serverCallbackHandler = serverCallbackHandler;
        this.tokensDir = tokensDir;
    }

    /*
     * RemotingSecurityProvider methods.
     */

    @Override
    public OptionMap getOptionMap() {
        List<String> mechanisms = new LinkedList<String>();
        Set<Property> properties = new HashSet<Property>();
        Builder builder = OptionMap.builder();

        mechanisms.add(JBOSS_LOCAL_USER);
        builder.set(SASL_POLICY_NOPLAINTEXT, false);
        properties.add(Property.of(LOCAL_DEFAULT_USER, DOLLAR_LOCAL));
        if (tokensDir != null) {
            properties.add(Property.of(LOCAL_USER_CHALLENGE_PATH, tokensDir));
        }
        if (digestMd5Supported()) {
            mechanisms.add(DIGEST_MD5);
            properties.add(Property.of(REALM_PROPERTY, realm.getName()));
            if (contains(DigestHashCallback.class, realm.getCallbackHandler().getSupportedCallbacks())) {
                properties.add(Property.of(PRE_DIGESTED_PROPERTY, Boolean.TRUE.toString()));
            }
        } else if (plainSupported()) {
            mechanisms.add(PLAIN);
        } else if (realm == null) {
            mechanisms.add(ANONYMOUS);
            builder.set(SASL_POLICY_NOANONYMOUS, false);
        } else {
            throw MESSAGES.noSupportingMechanismsForRealm();
        }

        SslMode sslMode = getSslMode();
        switch (sslMode) {
            case OFF:
                builder.set(SSL_ENABLED, false);
                break;
            case TRANSPORT_ONLY:
                builder.set(SSL_ENABLED, true);
                builder.set(SSL_STARTTLS, true);
                break;
            case CLIENT_AUTH_REQUESTED:
                builder.set(SSL_ENABLED, true);
                builder.set(SSL_STARTTLS, true);
                mechanisms.add(0, EXTERNAL);
                builder.set(SSL_CLIENT_AUTH_MODE, REQUESTED);
                break;
        // We do not currently support the SSL_CLIENT_AUTH_MODE of REQUIRED as there is always
        // the possibility that the local mechanism will still be needed.
        }

        builder.set(SASL_MECHANISMS, Sequence.of(mechanisms));
        builder.set(SASL_PROPERTIES, Sequence.of(properties));

        return builder.getMap();
    }

    @Override
    public ServerAuthenticationProvider getServerAuthenticationProvider() {
        return new ServerAuthenticationProvider() {

            @Override
            public CallbackHandler getCallbackHandler(String mechanismName) {
                return RealmSecurityProvider.this.getCallbackHandler(mechanismName);
            }
        };
    }

    @Override
    public XnioSsl getXnioSsl() {
        final SSLContext sslContext;
        if (realm == null || (sslContext = realm.getSSLContext()) == null) {
            return null;
        }

        return new JsseXnioSsl(Xnio.getInstance(Remoting.class.getClassLoader()), OptionMap.EMPTY, sslContext);
    }

    /*
     * Internal methods.
     */

    public CallbackHandler getCallbackHandler(String mechanismName) {
        // TODO - Once authorization is in place we may be able to relax the realm check to
        //        allow anonymous along side fully authenticated connections.

        // If the mechanism is ANONYMOUS and we don't have a realm we return quickly.
        if (ANONYMOUS.equals(mechanismName) && realm == null) {
            return new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        throw MESSAGES.anonymousMechanismNotExpected(current);
                    }
                }
            };
        }

        // For now for the JBOSS_LOCAL_USER we are only supporting the $local user and not allowing for
        // an alternative authorizationID.
        if (JBOSS_LOCAL_USER.equals(mechanismName)) {
            return new CallbackHandler() {

                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        if (current instanceof NameCallback) {
                            NameCallback ncb = (NameCallback) current;
                            if (!DOLLAR_LOCAL.equals(ncb.getDefaultName())) {
                                throw MESSAGES.onlyLocalUserIsAcceptable(DOLLAR_LOCAL);
                            }
                        } else if (current instanceof AuthorizeCallback) {
                            AuthorizeCallback acb = (AuthorizeCallback) current;
                            acb.setAuthorized(acb.getAuthenticationID().equals(acb.getAuthorizationID()));
                        } else {
                            throw MESSAGES.unsupportedCallback(current);
                        }
                    }

                }
            };
        }

        // In this calls only the AuthorizeCallback is needed, we are not making use if an authorization ID just yet
        // so don't need to be linked back to the realms.
        if (EXTERNAL.equals(mechanismName)) {
            return new CallbackHandler() {

                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        if (current instanceof AuthorizeCallback) {
                            AuthorizeCallback acb = (AuthorizeCallback) current;
                            acb.setAuthorized(acb.getAuthenticationID().equals(acb.getAuthorizationID()));
                        } else {
                            throw MESSAGES.unsupportedCallback(current);
                        }
                    }

                }
            };
        }

        final CallbackHandler realmCallbackHandler; // Referenced later by an inner-class so needs to be final.

        // We must have a match in this block or throw an IllegalStateException.
        if (DIGEST_MD5.equals(mechanismName) && digestMd5Supported() ||
                PLAIN.equals(mechanismName) && plainSupported()) {
            realmCallbackHandler = realm.getCallbackHandler();
        } else {
            return null;
        }

        // If there is not serverCallbackHandler then we don't need to wrap it so we can just return the realm
        // name fix handler which is already wrapping the real handler.
        if (serverCallbackHandler == null) {
            return realmCallbackHandler;
        }

        return new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                serverCallbackHandler.handle(callbacks);
                if (handled(callbacks) == false) {
                    realmCallbackHandler.handle(callbacks);
                }
            }

            /*
            * Check if the PasswordCallback had already been handled.
            */
            private boolean handled(Callback[] callbacks) {
                // For the moment handled will only return true if the user was successfully authenticated,
                // may later add a Callback to obtain the server info to verify is a server was identified with
                // the specified username.

                for (Callback current : callbacks) {
                    if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        char[] password = pcb.getPassword();
                        return (password != null && password.length > 0);
                    } else if (current instanceof VerifyPasswordCallback) {
                        return ((VerifyPasswordCallback) current).isVerified();
                    } else if (current instanceof DigestHashCallback) {
                        return ((DigestHashCallback) current).getHash() != null;
                    }
                }
                return false;
            }
        };
    }

    private SslMode getSslMode() {
        if (realm == null || realm.getSSLContext() == null) {
            // No SSL Context to no way can we enable SSL.
            return SslMode.OFF;
        }

        if (realm.hasTrustStore()) {
            return SslMode.CLIENT_AUTH_REQUESTED;
        }

        return SslMode.TRANSPORT_ONLY;
    }

    private enum SslMode {
        OFF, TRANSPORT_ONLY, CLIENT_AUTH_REQUESTED
    }

    private boolean digestMd5Supported() {
        if (realm == null) {
            return false;
        }

        Class[] callbacks = realm.getCallbackHandler().getSupportedCallbacks();
        if (contains(NameCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(RealmCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(PasswordCallback.class, callbacks) == false &&
                contains(DigestHashCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(AuthorizeCallback.class, callbacks) == false) {
            return false;
        }

        return true;
    }

    private boolean plainSupported() {
        if (realm == null) {
            return false;
        }

        Class[] callbacks = realm.getCallbackHandler().getSupportedCallbacks();
        if (contains(NameCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(VerifyPasswordCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(AuthorizeCallback.class, callbacks) == false) {
            return false;
        }

        return true;
    }

    private static boolean contains(Class clazz, Class[] classes) {
        for (Class current : classes) {
            if (current.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

}

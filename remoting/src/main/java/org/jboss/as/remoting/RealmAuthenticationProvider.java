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

import static org.xnio.Options.SASL_MECHANISMS;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import static org.xnio.Options.SASL_PROPERTIES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;

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
public class RealmAuthenticationProvider implements ServerAuthenticationProvider {

    static final String REALM_PROPERTY = "com.sun.security.sasl.digest.realm";
    static final String PRE_DIGESTED_PROPERTY = "org.jboss.sasl.digest.pre_digested";

    static final String ANONYMOUS = "ANONYMOUS";

    static final String DIGEST_MD5 = "DIGEST-MD5";

    static final String PLAIN = "PLAIN";

    private final SecurityRealm realm;
    private final CallbackHandler serverCallbackHandler;

    public RealmAuthenticationProvider(final SecurityRealm realm, final CallbackHandler serverCallbackHandler) {
        this.realm = realm;
        this.serverCallbackHandler = serverCallbackHandler;
    }

    OptionMap getSaslOptionMap() {
        if (digestMd5Supported()) {
            Builder builder = OptionMap.builder();
            builder.set(SASL_MECHANISMS, Sequence.of("DIGEST-MD5"));

            Sequence<Property> properties;
            if (contains(DigestHashCallback.class, realm.getCallbackHandler().getSupportedCallbacks())) {
                properties = Sequence.of(Property.of(REALM_PROPERTY, realm.getName()), Property.of(PRE_DIGESTED_PROPERTY, Boolean.TRUE.toString()));
            } else {
                properties = Sequence.of(Property.of(REALM_PROPERTY, realm.getName()));
            }

            builder.set(SASL_PROPERTIES, properties);

            return builder.getMap();
        }

        if (plainSupported()) {
            if (true)
                throw new IllegalStateException("PLAIN not enabled until SSL supported for Native Interface");

            return OptionMap.create(Options.SASL_MECHANISMS, Sequence.of(PLAIN), SASL_POLICY_NOPLAINTEXT, false);
        }

        if (realm == null) {
            return OptionMap.create(SASL_MECHANISMS, Sequence.of(ANONYMOUS), SASL_POLICY_NOANONYMOUS, Boolean.FALSE);
        }

        throw new IllegalStateException("A security realm has been specified but no supported mechanism identified.");
    }

    public CallbackHandler getCallbackHandler(String mechanismName) {
        // If the mechanism is ANONYMOUS and we don't have a realm we return quickly.
        if (ANONYMOUS.equals(mechanismName) && realm == null) {
            return new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        throw new UnsupportedCallbackException(current, "ANONYMOUS mechanism so not expecting a callback");
                    }
                }
            };
        }

        CallbackHandler realmCallbackHandler = null;

        // We must have a match in this block or throw an IllegalStateException.
        if (DIGEST_MD5.equals(mechanismName) && digestMd5Supported() ||
                PLAIN.equals(mechanismName) && plainSupported()) {
            realmCallbackHandler = realm.getCallbackHandler();
        } else {
            throw new IllegalStateException("Unsupported Callback '" + mechanismName + "'");
        }

        // If there is not serverCallbackHandler then we don't need to wrap it so we can just return the realm
        // name fix handler which is already wrapping the real handler.
        if (serverCallbackHandler == null) {
            return realmCallbackHandler;
        }

        final CallbackHandler wrappedHandler = realmCallbackHandler; // Just a copy so it can be made final for the inner class.
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                serverCallbackHandler.handle(callbacks);
                if (handled(callbacks) == false) {
                    wrappedHandler.handle(callbacks);
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

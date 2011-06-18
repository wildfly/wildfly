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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.xnio.OptionMap;
import org.xnio.Options;
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
class RealmAuthenticationProvider implements ServerAuthenticationProvider {

    static final String DIGEST_MD5 = "DIGEST-MD5";

    private final SecurityRealm realm;

    RealmAuthenticationProvider(final SecurityRealm realm) {
        this.realm = realm;
    }

    OptionMap getSaslOptionMap() {
        if (digestMd5Supported()) {
            return OptionMap.create(Options.SASL_MECHANISMS, Sequence.of("DIGEST-MD5"));
        }

        return OptionMap.EMPTY;
    }

    public CallbackHandler getCallbackHandler(String mechanismName) {
        if (DIGEST_MD5.equals(mechanismName) && digestMd5Supported()) {
            final CallbackHandler realHandler = realm.getCallbackHandler();
            // TODO - Correct JBoss Remoting so that the realm can be specified independently of the endpoint name.
            // In the meantime
            return new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    List<Callback> filteredCallbacks = new ArrayList<Callback>(callbacks.length - 1);
                    for (Callback current : callbacks) {
                        if (current instanceof RealmCallback == false) {
                            filteredCallbacks.add(current);
                        }
                    }
                    realHandler.handle(filteredCallbacks.toArray(new Callback[filteredCallbacks.size()]));

                }

            };
        }

        return null;
    }

    private boolean digestMd5Supported() {
        Class[] callbacks = realm.getCallbackHandler().getSupportedCallbacks();
        if (contains(NameCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(RealmCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(PasswordCallback.class, callbacks) == false) {
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

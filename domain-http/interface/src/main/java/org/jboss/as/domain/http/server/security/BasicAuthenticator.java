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
package org.jboss.as.domain.http.server.security;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;

import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpPrincipal;
import org.jboss.com.sun.net.httpserver.HttpsExchange;
import org.jboss.sasl.callback.VerifyPasswordCallback;

import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class BasicAuthenticator extends org.jboss.com.sun.net.httpserver.BasicAuthenticator {

    private final CallbackHandler callbackHandler;

    private final boolean verifyPasswordCallback;

    public BasicAuthenticator(DomainCallbackHandler callbackHandler, String realm) {
        super(realm);
        this.callbackHandler = callbackHandler;
        verifyPasswordCallback = contains(VerifyPasswordCallback.class, callbackHandler.getSupportedCallbacks());
    }

    @Override
    public Result authenticate(HttpExchange httpExchange) {
        // If we already have a Principal from the SSLSession no need to continue with
        // username / password authentication.
        if (httpExchange instanceof HttpsExchange) {
            HttpsExchange httpsExch = (HttpsExchange) httpExchange;
            SSLSession session = httpsExch.getSSLSession();
            if (session != null) {
                try {
                    Principal p = session.getPeerPrincipal();

                    return new Success(new HttpPrincipal(p.getName(), realm));

                } catch (SSLPeerUnverifiedException e) {
                }
            }
        }

        return super.authenticate(httpExchange);
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        NameCallback ncb = new NameCallback("Username", username);
        Callback passwordCallback;
        if (verifyPasswordCallback) {
            passwordCallback = new VerifyPasswordCallback(password);
        } else {
            passwordCallback = new PasswordCallback("Password", false);
        }
        Callback[] callbacks = new Callback[]{ncb, passwordCallback};

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException e) {
            ROOT_LOGGER.debug("Callback handle failed.", e);
            return false;
        } catch (UnsupportedCallbackException e) {
            throw MESSAGES.callbackRejected(e);
        }

        if (verifyPasswordCallback) {
            return ((VerifyPasswordCallback) passwordCallback).isVerified();
        } else {
            char[] expectedPassword = ((PasswordCallback) passwordCallback).getPassword();

            return Arrays.equals(password.toCharArray(), expectedPassword);
        }
    }

    // TODO - Will do something cleaner with collections.
    public static boolean requiredCallbacksSupported(Class[] callbacks) {
        if (contains(NameCallback.class,callbacks) == false) {
            return false;
        }
        if ( (contains(PasswordCallback.class,callbacks) == false) && (contains(VerifyPasswordCallback.class,callbacks) == false)) {
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

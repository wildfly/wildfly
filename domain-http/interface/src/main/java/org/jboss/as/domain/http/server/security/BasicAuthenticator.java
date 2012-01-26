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

import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.as.domain.management.SubjectUserInfo;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpExchange.AttributeScope;
import org.jboss.com.sun.net.httpserver.HttpPrincipal;
import org.jboss.com.sun.net.httpserver.HttpsExchange;
import org.jboss.sasl.callback.VerifyPasswordCallback;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class BasicAuthenticator extends org.jboss.com.sun.net.httpserver.BasicAuthenticator {

    private final AuthenticationProvider authenticationProvider;

    private final boolean verifyPasswordCallback;
    private final ThreadLocal<AuthorizingCallbackHandler> callbackHandler = new ThreadLocal<AuthorizingCallbackHandler>();

    public BasicAuthenticator(AuthenticationProvider authenticationProvider, String realm) {
        super(realm);
        this.authenticationProvider = authenticationProvider;
        verifyPasswordCallback = contains(VerifyPasswordCallback.class, authenticationProvider.getCallbackHandler().getSupportedCallbacks());
    }

    @Override
    public Result authenticate(HttpExchange httpExchange) {
        Subject subject = (Subject) httpExchange.getAttribute(Subject.class.getName(), AttributeScope.CONNECTION);
        // If we have a cached Subject with a HttpPrincipal this connection is already authenticated so no further action
        // required.
        if (subject != null) {
            Set<HttpPrincipal> httpPrincipals = subject.getPrincipals(HttpPrincipal.class);
            if (httpPrincipals.size() > 0) {
                return new Success(httpPrincipals.iterator().next());
            }
        }

        callbackHandler.set(authenticationProvider.getCallbackHandler());
        try {
            return _authenticate(httpExchange);
        } finally {
            callbackHandler.set(null);
        }
    }

    private Result _authenticate(HttpExchange httpExchange) {
        Result response = null;
        // If we already have a Principal from the SSLSession no need to continue with
        // username / password authentication.
        if (httpExchange instanceof HttpsExchange) {
            HttpsExchange httpsExch = (HttpsExchange) httpExchange;
            SSLSession session = httpsExch.getSSLSession();
            if (session != null) {
                try {
                    Principal p = session.getPeerPrincipal();

                    response = new Success(new HttpPrincipal(p.getName(), realm));
                } catch (SSLPeerUnverifiedException e) {
                }
            }
        }
        if (response == null) {
            response = super.authenticate(httpExchange);
        }

        if (response instanceof Success) {
            // For this method to have been called a Subject with HttpPrincipal was not found within the HttpExchange so now
            // create a new one.
            HttpPrincipal principal = ((Success) response).getPrincipal();

            try {
                SubjectUserInfo userInfo = callbackHandler.get().createSubjectUserInfo(principal);
                httpExchange.setAttribute(Subject.class.getName(), userInfo.getSubject());

            } catch (IOException e) {
                ROOT_LOGGER.debug("Unable to create SubjectUserInfo", e);
                response = new Authenticator.Failure(INTERNAL_SERVER_ERROR);
            }
        }

        return response;
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
            callbackHandler.get().handle(callbacks);
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

    public static boolean requiredCallbacksSupported(Class<Callback>[] callbacks) {
        if (contains(NameCallback.class,callbacks) == false) {
            return false;
        }
        if ( (contains(PasswordCallback.class,callbacks) == false) && (contains(VerifyPasswordCallback.class,callbacks) == false)) {
            return false;
        }

        return true;
    }

    private static boolean contains(Class clazz, Class<Callback>[] classes) {
        for (Class<Callback> current : classes) {
            if (current.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

}

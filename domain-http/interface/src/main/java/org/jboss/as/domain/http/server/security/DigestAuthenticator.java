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

import static org.jboss.as.domain.http.server.Constants.AUTHORIZATION_HEADER;
import static org.jboss.as.domain.http.server.Constants.UNAUTHORIZED;
import static org.jboss.as.domain.http.server.Constants.VIA;
import static org.jboss.as.domain.http.server.Constants.WWW_AUTHENTICATE_HEADER;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.domain.management.security.UserNotFoundException;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpPrincipal;
import org.jboss.com.sun.net.httpserver.HttpsExchange;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.util.HexConverter;

/**
 * An authenticator to handle Digest authentication.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DigestAuthenticator extends Authenticator {

    private final NonceFactory nonceFactory = new NonceFactory();

    private final CallbackHandler callbackHandler;

    private final String realm;
    private final boolean preDigested;

    private static final byte COLON = ':';
    private static final String CHALLENGE = "Digest";
    private static final String NONCE = "nonce";
    private static final String MD5 = "MD5";
    private static final String REALM = "realm";
    private static final String RESPONSE = "response";
    private static final String USERNAME = "username";
    private static final String URI = "uri";

    public DigestAuthenticator(CallbackHandler callbackHandler, String realm, boolean preDigested) {
        this.callbackHandler = callbackHandler;
        this.realm = realm;
        this.preDigested = preDigested;
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

        // If authentication has already completed for this connection re-use it.
        DigestContext context = getOrCreateNegotiationContext(httpExchange);
        if (context.isAuthenticated()) {
            return new Authenticator.Success(context.getPrincipal());
        }

        // No previous authentication so time to continue the process.
        Headers requestHeaders = httpExchange.getRequestHeaders();
        if (requestHeaders.containsKey(AUTHORIZATION_HEADER) == false) {
            Headers responseHeaders = httpExchange.getResponseHeaders();

            responseHeaders.add(WWW_AUTHENTICATE_HEADER, CHALLENGE + " " + createChallenge(false));

            return new Authenticator.Retry(UNAUTHORIZED);
        }

        String authorizationHeader = requestHeaders.getFirst(AUTHORIZATION_HEADER);
        if (authorizationHeader.startsWith(CHALLENGE + " ") == false) {
            throw MESSAGES.invalidAuthorizationHeader();
        }
        String challenge = authorizationHeader.substring(CHALLENGE.length() + 1);
        Map<String, String> challengeParameters = parseDigestChallenge(challenge);

        // Validate Challenge, expect one of 3 responses VALID, INVALID, STALE

        HttpPrincipal principal = validateUser(httpExchange, challengeParameters);

        // INVALID - Username / Password verification failed - Nonce is irrelevant.
        if (principal == null) {
            if (challengeParameters.containsKey(NONCE)) {
                nonceFactory.useNonce(challengeParameters.get(NONCE));
            }

            Headers responseHeaders = httpExchange.getResponseHeaders();
            responseHeaders.add(WWW_AUTHENTICATE_HEADER, CHALLENGE + " " + createChallenge(false));
            return new Authenticator.Retry(UNAUTHORIZED);
        }

        // VALID - Verified username and password, Nonce is correct.
        if (nonceFactory.useNonce(challengeParameters.get(NONCE))) {
            context.principal = principal;

            return new Authenticator.Success(principal);
        }

        // STALE - Verification of username and password succeeded but Nonce now stale.
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.add(WWW_AUTHENTICATE_HEADER, CHALLENGE + " " + createChallenge(true));
        return new Authenticator.Retry(UNAUTHORIZED);
    }

    private HttpPrincipal validateUser(HttpExchange httpExchange, Map<String, String> challengeParameters) {
        String realm = challengeParameters.get(REALM);
        String username = challengeParameters.get(USERNAME);

        if (realm == null || realm.length() == 0 || username == null || username.length() == 0) {
            // Fail quickly if either the realm or username are not supplied.
            return null;
        }

        // Step 1 - Create Callbacks
        RealmCallback rcb = new RealmCallback("Realm", realm);
        NameCallback ncb = new NameCallback("Username", username);
        Callback credentialCallback = preDigested ? new DigestHashCallback("Password Digest") : new PasswordCallback("Password", false);
        Callback[] callbacks = new Callback[]{rcb, ncb, credentialCallback};

        // Step 2 - Call CallbackHandler
        try {
            callbackHandler.handle(callbacks);
        } catch (UserNotFoundException e) {
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER.debug(e.getMessage());
            }
            return null;
        } catch (IOException e) {
            throw MESSAGES.invalidCallbackHandler();
        } catch (UnsupportedCallbackException e) {
            throw MESSAGES.invalidCallbackHandler();
        }

        // TODO - Verify that a password was set (Depending on if multiple CallbackHandlers are supported)

        // Step 3 - Generate MD5 and Compare
        try {
            // TODO - The remaining combinations from RFC-2617 need to be added.
            // TODO - Verify all required parameters were set.
            MessageDigest md = MessageDigest.getInstance(MD5);
            byte[] ha1;
            if (preDigested) {
                DigestHashCallback dhc = (DigestHashCallback) credentialCallback;

                ha1 = dhc.getHexHash().getBytes();
            } else {
                md.update(challengeParameters.get(USERNAME).getBytes());
                md.update(COLON);
                md.update(challengeParameters.get(REALM).getBytes());
                md.update(COLON);
                PasswordCallback pcb = (PasswordCallback) credentialCallback;
                md.update(new String(pcb.getPassword()).getBytes());

                ha1 = HexConverter.convertToHexBytes(md.digest());
            }

            md.update(httpExchange.getRequestMethod().getBytes());
            md.update(COLON);
            md.update(challengeParameters.get(URI).getBytes());

            byte[] ha2 = HexConverter.convertToHexBytes(md.digest());

            md.update(ha1);
            md.update(COLON);
            md.update(challengeParameters.get(NONCE).getBytes());
            md.update(COLON);
            md.update(ha2);

            byte[] expectedResponse = HexConverter.convertToHexBytes(md.digest());
            byte[] actualResponse = challengeParameters.get(RESPONSE).getBytes();

            if (MessageDigest.isEqual(expectedResponse, actualResponse)) {
                return new HttpPrincipal(challengeParameters.get(USERNAME), challengeParameters.get(REALM));
            }

        } catch (NoSuchAlgorithmException e) {
            throw MESSAGES.md5Unavailable(e);
        }

        return null;
    }


    private String createChallenge(boolean stale) {
        StringBuilder challenge = new StringBuilder();
        challenge.append("realm=\"").append(realm).append("\",");
        challenge.append("nonce=\"").append(nonceFactory.createNonce()).append("\"");
        if (stale == true) {
            challenge.append(",stale=true");
        }
        return challenge.toString();
    }

    private Map<String, String> parseDigestChallenge(String challenge) {
        Map<String, String> response = new HashMap<String, String>();
        HeaderParser parser = new HeaderParser(challenge);
        while (parser.hasNext()) {
            HeaderParser.Parameter next = parser.next();
            response.put(next.key, next.value);
        }

        return response;
    }


    private class HeaderParser {

        private static final char EQUALS = '=';

        private static final char DELIMITER = ',';

        private static final char QUOTE = '"';

        private static final char ESCAPE = '\\';

        private final String message;
        private final int length;
        private int pos = 0;
        private boolean hasNextConfirmed;

        HeaderParser(String message) {
            this.message = message;
            this.length = message.length();
        }

        /**
         * @return true if there is another key/value parameter to return.
         */
        boolean hasNext() {
            if (hasNextConfirmed == true) {
                return true;
            }

            // Check pos not at end.
            if (pos >= length) {
                return false;
            }

            // Check there is an EQUALS also not at end
            int nextEquals = message.indexOf(EQUALS, pos);
            if (nextEquals < 0 || nextEquals >= length - 1) {
                return false;
            }

            hasNextConfirmed = true;
            return true;
        }

        /**
         * Parses and returns the next parameter from the header.
         *
         * @return The next Parameter or null if no further parameter.
         */
        Parameter next() {
            if (hasNextConfirmed == false && hasNext() == false) {
                return null;
            }
            Parameter response = new Parameter();
            // Find the key.
            int equalsPos = message.indexOf(EQUALS, pos);
            response.key = message.substring(pos, equalsPos).trim();
            pos = equalsPos + 1;

            // Find the value.
            int nextDelimiter = message.indexOf(DELIMITER, pos);
            int nextQuote = message.indexOf(QUOTE, pos);
            boolean quoted = false;
            // Is there a quote and no further parameters or is there a quote before the
            // next delimiter?
            if (nextQuote > 0 && (nextDelimiter < 0 || nextQuote < nextDelimiter)) {
                quoted = true;
            }

            if (quoted == true) {
                // Check not dropping any random chars.
                String dropping = message.substring(pos, nextQuote).trim();
                if ("".equals(dropping) == false) {
                    throw MESSAGES.unexpectedHeaderChar(dropping, response.key);
                }
                pos = nextQuote;
                int endQuote = -1;
                while (endQuote < 0) {
                    nextQuote = message.indexOf(QUOTE, nextQuote + 1);
                    if (nextQuote < 0) {
                        throw MESSAGES.missingClosingQuote(response.key);
                    }
                    if (message.charAt(nextQuote - 1) != ESCAPE) {
                        endQuote = nextQuote;
                    }
                }
                // Don't trim as was a quoted value.
                response.value = message.substring(pos + 1, endQuote);

                // Move pos after DELIMITER.
                int nextDelimeter = message.indexOf(DELIMITER, pos);
                if (nextDelimeter > 0) {
                    pos = nextDelimeter + 1;
                }
            } else {
                int nextDelimeter = message.indexOf(DELIMITER, pos);
                if (nextDelimeter > 0) {
                    response.value = message.substring(pos, nextDelimeter).trim();
                    // Move pos after DELIMITER.
                    pos = nextDelimeter + 1;
                } else {
                    response.value = message.substring(pos, length - 1).trim();
                    // Set pos to end of message.
                    pos = length + 1;
                }

            }

            hasNextConfirmed = false;
            return response;
        }


        class Parameter {
            String key;
            String value;
        }

    }


    private DigestContext getOrCreateNegotiationContext(HttpExchange httpExchange) {
        Headers headers = httpExchange.getRequestHeaders();
        boolean proxied = headers.containsKey(VIA);

        if (proxied) {
            return new DigestContext();
        } else {
            DigestContext context = (DigestContext) httpExchange.getAttribute(DigestContext.KEY, HttpExchange.AttributeScope.CONNECTION);
            if (context == null) {
                context = new DigestContext();
                httpExchange.setAttribute(DigestContext.KEY, context, HttpExchange.AttributeScope.CONNECTION);
            }
            return context;
        }

    }

    private class DigestContext {

        private static final String KEY = "DIGEST_CONTEXT";

        private HttpPrincipal principal = null;

        boolean isAuthenticated() {
            return principal != null;
        }

        HttpPrincipal getPrincipal() {
            return principal;
        }

    }

    // TODO - Will do something cleaner with collections.
    public static boolean requiredCallbacksSupported(Class[] callbacks) {
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

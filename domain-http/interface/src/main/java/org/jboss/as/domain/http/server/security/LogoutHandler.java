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

package org.jboss.as.domain.http.server.security;

import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.HOST;
import static io.undertow.util.Headers.LOCATION;
import static io.undertow.util.Headers.REFERER;
import static io.undertow.util.Headers.USER_AGENT;
import io.undertow.io.IoCallback;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.idm.DigestAlgorithm;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.security.impl.DigestAuthenticationMechanism;
import io.undertow.security.impl.DigestQop;
import io.undertow.security.impl.SimpleNonceManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.FlexBase64;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LogoutHandler implements HttpHandler {

    public static final String PATH = "/logout";
    public static final String CONTEXT = "org.jboss.as.console.logout.context";
    private static final String EXIT = "org.jboss.as.console.logout.exit";

    private static final String HIT_ESCAPE = "HIT THE ESCAPE KEY";
    private static final String BASIC = "BASIC";
    private static final String DIGEST = "DIGEST";
    private static final String MECHANISM = "mechanism";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final DigestAuthenticationMechanism digestMechanism;
    private final DigestAuthenticationMechanism fakeRealmdigestMechanism;
    private final BasicAuthenticationMechanism basicMechanism;
    private final BasicAuthenticationMechanism fakeRealmBasicMechanism;

    public LogoutHandler(final String realmName) {
        List<DigestAlgorithm> digestAlgorithms = Collections.singletonList(DigestAlgorithm.MD5);
        List<DigestQop> digestQops = Collections.emptyList();
        digestMechanism = new DigestAuthenticationMechanism(digestAlgorithms, digestQops, realmName, "/management",
                new SimpleNonceManager());
        fakeRealmdigestMechanism = new DigestAuthenticationMechanism(digestAlgorithms, digestQops, HIT_ESCAPE,
                "/management", new SimpleNonceManager());
        basicMechanism = new BasicAuthenticationMechanism(realmName);
        fakeRealmBasicMechanism = new BasicAuthenticationMechanism(HIT_ESCAPE);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        String referrer = responseHeaders.getFirst(REFERER);
        String protocol = exchange.getRequestScheme();
        String host = null;
        if (referrer != null) {
            try {
                URI uri = new URI(referrer);
                protocol = uri.getScheme();
                host = uri.getHost() + portPortion(protocol, uri.getPort());
            } catch (URISyntaxException e) {
            }
        }
        if (host == null) {
            host = requestHeaders.getFirst(HOST);
            if (host == null) {
                exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
                return;
            }
        }

        /*
         * Main sequence of events:
         *
         * 1. Redirect to self using user:pass@host form of authority. This forces Safari to overwrite its cache. (Also
         * forces FF and Chrome, but not absolutely necessary) Set the exit flag as a state signal for step 3
         *
         * 2. Send 401 digest without a nonce stale marker, this will force FF and Chrome and likely other browsers to
         * assume an invalid (old) password. In the case of Opera, which doesn't invalidate under such a circumstance,
         * send an invalid realm. This will overwrite its auth cache, since it indexes it by host and not realm.
         *
         * 3. The credentials in 307 redirect wlll be transparently accepted and a final redirect to the console is
         * performed. Opera ignores these, so the user must hit escape which will use javascript to perform the redirect
         *
         * In the case of Internet Explorer, all of this will be bypassed and will simply redirect to the console. The console
         * MUST use a special javascript call before redirecting to logout.
         */
        String userAgent = requestHeaders.getFirst(USER_AGENT);
        boolean opera = userAgent != null && userAgent.contains("Opera");
        boolean win = !opera && userAgent != null && userAgent.contains("MSIE");

        String rawQuery = exchange.getQueryString();
        boolean exit = rawQuery != null && rawQuery.contains(EXIT);



        if (win) {
            responseHeaders.add(LOCATION, protocol + "://" + host + "/");
            exchange.setResponseCode(StatusCodes.TEMPORARY_REDIRECT);
        } else {
            // Do the redirects to finish the logout
            String authorization = requestHeaders.getFirst(AUTHORIZATION);

            boolean digest = true;
            Map<String, Deque<String>> parameters = exchange.getQueryParameters();
            if (parameters.containsKey(MECHANISM)) {
                digest = !BASIC.equals(parameters.get(MECHANISM).getFirst());
            }
            if (authorization != null && authorization.length() > BASIC.length()
                    && BASIC.equalsIgnoreCase(authorization.substring(0, BASIC.length()))) {
                digest = false;
                ByteBuffer decode = FlexBase64.decode(authorization.substring(6));
                authorization = new String(decode.array(), decode.arrayOffset(), decode.limit(), UTF_8);
            }

            if (authorization == null || !authorization.contains("enter-login-here")) {
                if (!exit) {
                    responseHeaders.add(LOCATION, protocol + "://enter-login-here:blah@" + host + "/logout?" + EXIT + "&"
                            + MECHANISM + "=" + (digest ? DIGEST : BASIC));
                    exchange.setResponseCode(StatusCodes.TEMPORARY_REDIRECT);
                    return;
                }

                mechanism(opera, digest).sendChallenge(exchange, null);
                String reply = "<html><script type='text/javascript'>window.location=\"" + protocol + "://" + host
                        + "/\";</script></html>";
                exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
                exchange.getResponseSender().send(reply, IoCallback.END_EXCHANGE);
                return;
            }

            // Success, now back to the login screen
            responseHeaders.add(LOCATION, protocol + "://" + host + "/");
            exchange.setResponseCode(StatusCodes.TEMPORARY_REDIRECT);
        }
    }

    private AuthenticationMechanism mechanism(final boolean opera, final boolean digest) {
        if (digest) {
            return opera ? fakeRealmdigestMechanism : digestMechanism;
        } else {
            return opera ? fakeRealmBasicMechanism : basicMechanism;
        }
    }

    private String portPortion(final String scheme, final int port) {
        if (port == -1 || "http".equals(scheme) && port == 80 || "https".equals(scheme) && port == 443) {
            return "";
        }

        return ":" + String.valueOf(port);
    }
}
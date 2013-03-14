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
import io.undertow.security.impl.DigestAlgorithm;
import io.undertow.security.impl.DigestAuthenticationMechanism;
import io.undertow.security.impl.DigestQop;
import io.undertow.security.impl.SimpleNonceManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpHandlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.domain.http.server.Common;

/**
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LogoutHandler implements HttpHandler {

    public static final String PATH = "/logout";

    private final DigestAuthenticationMechanism digestMechanism;
    private final DigestAuthenticationMechanism fakeRealmdigestMechanism;

    public LogoutHandler(final String realmName) {
        List<DigestAlgorithm> digestAlgorithms = Collections.singletonList(DigestAlgorithm.MD5);
        List<DigestQop> digestQops = Collections.emptyList();
        digestMechanism = new DigestAuthenticationMechanism(digestAlgorithms, digestQops, realmName, "/management",
                new SimpleNonceManager(), false);
        fakeRealmdigestMechanism = new DigestAuthenticationMechanism(digestAlgorithms, digestQops, "HIT THE ESCAPE KEY",
                "/management", new SimpleNonceManager(), false);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        String authorization = requestHeaders.getFirst(AUTHORIZATION);
        String rawQuery = exchange.getQueryString();
        boolean query = rawQuery != null && rawQuery.contains("logout");

        String userAgent = requestHeaders.getFirst(USER_AGENT);
        boolean opera = userAgent != null && userAgent.contains("Opera");
        boolean win = !opera && userAgent != null && userAgent.contains("MSIE");

        String referrer = responseHeaders.getFirst(REFERER);

        // Calculate location URL
        String protocol = "http";
        String host = null;
        if (referrer != null) {
            try {
                URI uri = new URI(referrer);
                protocol = uri.getScheme();
                host = uri.getHost() + (uri.getPort() == -1 ? "" : ":" + String.valueOf(uri.getPort()));
            } catch (URISyntaxException e) {
            }
        }

        // Last resort
        if (host == null) {
            host = requestHeaders.getFirst(HOST);
            if (host == null) {
                Common.INTERNAL_SERVER_ERROR.handleRequest(exchange);

                return;
            }
        }
        /*
         * Main sequence of events:
         *
         * 1. Redirect to self using user:pass@host form of authority. This forces Safari to overwrite its cache. (Also forces
         * FF and Chrome, but not absolutely necessary) Set the logout query param as a state signal for step 2 2. Send 401
         * digest without a nonce stale marker, this will force FF and Chrome and likely other browsers to assume an invalid
         * (old) password. In the case of Opera, which doesn't invalidate under such a circumstance, send an invalid realm. This
         * will overwrite its auth cache, since it indexes it by host and not realm. 3. The credentials in 307 redirect wlll be
         * transparently accepted and a final redirect to the console is performed. Opera ignores these, so the user must hit
         * escape which will use javascript to perform the redirect
         *
         * In the case of Internet Explorer, all of this will be bypassed and will simply redirect to the console. The console
         * MUST use a special javascript call before redirecting to logout.
         */
        if (!win && (authorization == null || !authorization.contains("enter-login-here"))) {
            if (!query) {
                responseHeaders.add(LOCATION, protocol + "://enter-login-here:blah@" + host + "/logout?logout");
                HttpHandlers.executeHandler(Common.TEMPORARY_REDIRECT, exchange);
                return;
            }

            DigestAuthenticationMechanism mech = opera ? fakeRealmdigestMechanism : digestMechanism;
            mech.sendChallenge(exchange, null);
            PrintStream print = new PrintStream(exchange.getOutputStream());
            print.println("<html><script type='text/javascript'>window.location=\"" + protocol + "://" + host
                    + "/\";</script></html>");
            print.flush();
            print.close();
            HttpHandlers.executeHandler(Common.UNAUTHORIZED, exchange);

            return;
        }

        // Success, now back to the login screen
        responseHeaders.add(LOCATION, protocol + "://" + host + "/");
        HttpHandlers.executeHandler(Common.TEMPORARY_REDIRECT, exchange);
    }
}
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

package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.DomainUtil.constructUrl;
import static org.jboss.as.domain.http.server.Constants.AUTHORIZATION_HEADER;
import static org.jboss.as.domain.http.server.Constants.REFERER;
import static org.jboss.as.domain.http.server.Constants.USER_AGENT;
import static org.jboss.as.domain.http.server.Constants.HTTP;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.TEMPORARY_REDIRECT;
import static org.jboss.as.domain.http.server.Constants.WWW_AUTHENTICATE_HEADER;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.as.domain.http.server.security.DigestAuthenticator;
import org.jboss.as.domain.http.server.security.NonceFactory;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;

/**
* @author Jason T. Greene
*/
class LogoutHandler implements ManagementHttpHandler {
    private NonceFactory nonceFactory = new NonceFactory();
    private String realm;

    @Override
    public void start(HttpServer httpServer, SecurityRealm securityRealm) {
        httpServer.createContext("/logout", this);
        realm = securityRealm != null ? securityRealm.getName() : null;
    }

    @Override
    public void stop(HttpServer httpServer) {
        httpServer.removeContext("/logout");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final Headers requestHeaders = exchange.getRequestHeaders();
        final Headers responseHeaders = exchange.getResponseHeaders();

        // Redirect back if there is no realm to log out of
        if (realm == null) {
            responseHeaders.set(LOCATION, constructUrl(exchange, "/"));
            exchange.sendResponseHeaders(TEMPORARY_REDIRECT, -1);
        }

        String authorization = requestHeaders.getFirst(AUTHORIZATION_HEADER);
        String rawQuery = exchange.getRequestURI().getRawQuery();
        boolean query = rawQuery != null && rawQuery.contains("logout");

        String userAgent = requestHeaders.getFirst(USER_AGENT);
        boolean opera = userAgent != null && userAgent.contains("Opera");
        boolean win = !opera && userAgent != null && userAgent.contains("MSIE");

        String referrer = responseHeaders.getFirst(REFERER);

        // Calculate location URL
        String protocol = HTTP;
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
            host = requestHeaders.getFirst("Host");
            if (host == null) {
                exchange.sendResponseHeaders(500, -1);
                return;
            }
        }
        /*
         * Main sequence of events:
         *
         * 1. Redirect to self using user:pass@host form of authority. This forces Safari to overwrite
         *    its cache. (Also forces FF and Chrome, but not absolutely necessary)
         *    Set the logout query param as a state signal for step 2
         * 2. Send 401 digest without a nonce stale marker, this will force  FF and Chrome and likely
         *    other browsers to assume an invalid (old) password. In the case of Opera, which doesn't
         *    invalidate under such a circumstance, send an invalid realm. This will overwrite its
         *    auth cache, since it indexes it by host and not realm.
         * 3. The credentials in 307 redirect wlll be transparently accepted and a final redirect to
         *    the console is performed. Opera ignores these, so the user must hit escape which will
         *    use javascript to perform the redirect
         *
         * In the case of Internet Explorer, all of this will be bypassed and will simply redirect
         * to the console. The console MUST use a special javascript call before redirecting to
         * logout.
         *
         */
        if (!win && (authorization == null || !authorization.contains("enter-login-here"))) {
            if (! query) {
                responseHeaders.set(LOCATION, protocol + "://enter-login-here:blah@" + host + "/logout?logout");
                exchange.sendResponseHeaders(TEMPORARY_REDIRECT, -1);
                return;
            }

            String realm = opera ? "HIT THE ESCAPE KEY" : this.realm;
            DigestAuthenticator.DigestContext context = DigestAuthenticator.getOrCreateNegotiationContext(exchange, nonceFactory, false);
            responseHeaders.add(WWW_AUTHENTICATE_HEADER, "Digest " + DigestAuthenticator.createChallenge(context, realm, false));
            exchange.sendResponseHeaders(401, 0);
            PrintStream print = new PrintStream(exchange.getResponseBody());
            print.println("<html><script type='text/javascript'>window.location=\"" + protocol + "://" + host + "/\";</script></html>");
            print.flush();
            print.close();

            return;
        }

        // Success, now back to the login screen
        responseHeaders.set(LOCATION, protocol + "://" + host + "/");
        exchange.sendResponseHeaders(TEMPORARY_REDIRECT, -1);
    }
}

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

import static io.undertow.predicate.Predicates.not;
import static io.undertow.predicate.Predicates.path;
import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.HOST;
import static io.undertow.util.Headers.REFERER;
import static io.undertow.util.Headers.USER_AGENT;
import io.undertow.io.IoCallback;
import io.undertow.security.idm.DigestAlgorithm;
import io.undertow.security.impl.DigestAuthenticationMechanism;
import io.undertow.security.impl.DigestQop;
import io.undertow.security.impl.SimpleNonceManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LogoutHandler implements HttpHandler {

    public static final String PATH = "/logout";

    private static final String DOMAIN_HTTP_INTERFACE_MODULE = "org.jboss.as.domain-http-interface";
    private static final String DOMAIN_HTTP_INTERFACE_SLOT = "main";

    private final DigestAuthenticationMechanism digestMechanism;
    private final DigestAuthenticationMechanism fakeRealmdigestMechanism;

    public LogoutHandler(final String realmName) {
        List<DigestAlgorithm> digestAlgorithms = Collections.singletonList(DigestAlgorithm.MD5);
        List<DigestQop> digestQops = Collections.emptyList();
        digestMechanism = new DigestAuthenticationMechanism(digestAlgorithms, digestQops, realmName, "/management",
                new SimpleNonceManager());
        fakeRealmdigestMechanism = new DigestAuthenticationMechanism(digestAlgorithms, digestQops, "HIT THE ESCAPE KEY",
                "/management", new SimpleNonceManager());
    }

    public static HttpHandler createLogoutContext(String realmName) throws ModuleLoadException {
        final ClassPathResourceManager resource = new ClassPathResourceManager(getClassLoader(Module.getCallerModuleLoader(), DOMAIN_HTTP_INTERFACE_MODULE, DOMAIN_HTTP_INTERFACE_SLOT), "");
        final io.undertow.server.handlers.resource.ResourceHandler handler = new io.undertow.server.handlers.resource.ResourceHandler()
                .setCacheTime(60 * 60 * 24 * 31)
                .setAllowed(not(path("META-INF")))
                .setResourceManager(resource)
                .setDirectoryListingEnabled(false);

        LogoutHandler logoutHandler = new LogoutHandler(realmName);
        // If the request is for /Logout.html, use the resource handler, otherwise, use LogoutHandler itself
        PredicateHandler handleLogoutHTML = new PredicateHandler(path("/Logout.html"), handler, logoutHandler);
        // If the request is for "", redirect to /Logout.html, otherwise use the above handler
        PredicateHandler redirectToLogoutHTML = new PredicateHandler(path(""), new RedirectHandler(PATH + "/Logout.html"), handleLogoutHTML);
        return redirectToLogoutHTML;
    }

    private static ClassLoader getClassLoader(final ModuleLoader moduleLoader, final String module, final String slot) throws ModuleLoadException {
        ModuleIdentifier id = ModuleIdentifier.create(module, slot);
        ClassLoader cl = moduleLoader.loadModule(id).getClassLoader();

        return cl;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        String authorization = requestHeaders.getFirst(AUTHORIZATION);
        String action = exchange.getQueryParameters().get("action").getFirst();
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
                exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);

                return;
            }
        }

        /**
         * The basic flow here is:
         * 1. User views /logout/Logout.html
         * 2. Logout.html sends an ajax request to /logout/LogoutHandler?action=step1, while attempting to send fake credentials
         *      - This returns a 401
         *      - If the browser send the fake credentials, include a DigestAuthentication request
         *      - Otherwise, do not (this avoids some browsers popping up a login dialog)
         * 3. Logout.html sends another ajax request. Just reject it with a 401
         *      - This steps prevents some browsers (Chrome/Chromium) from eventually resurrecting past credentials
         * 4. Finally, the Logout.html page sends the step3 request. This is accepted with 200 OK, triggering the browser
         *    to replace its cache credentials on all future requests.
         * 5. Logout.html redirects back to / 
         */
        if (action.equalsIgnoreCase("step1")) {
            if (authorization == null) {
                DigestAuthenticationMechanism mech = opera ? fakeRealmdigestMechanism : digestMechanism;
                mech.sendChallenge(exchange, null);
            }
            exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseSender().send("Step 1 complete", IoCallback.END_EXCHANGE);
        } else if (action.equalsIgnoreCase("step2")) {
            exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseSender().send("Step 2 complete", IoCallback.END_EXCHANGE);
        } else if (action.equalsIgnoreCase("step3")) {
            exchange.setResponseCode(StatusCodes.OK);
            exchange.getResponseSender().send("Step 3 complete", IoCallback.END_EXCHANGE);
        }
    }
}
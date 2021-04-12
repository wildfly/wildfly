/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.util.StatusCodes;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.as.web.session.SimpleSessionIdentifierCodec;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.elytron.web.undertow.server.ElytronContextAssociationHandler;
import org.wildfly.elytron.web.undertow.server.ElytronHttpExchange;
import org.wildfly.httpclient.common.ElytronIdentityHandler;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HttpInvokerHostService implements Service {

    private static final String JSESSIONID = "JSESSIONID";

    private final Supplier<Host> host;
    private final Supplier<HttpAuthenticationFactory> httpAuthenticationFactory;
    private final Supplier<SecurityRealm> realmService;
    private final Supplier<PathHandler> remoteHttpInvokerService;
    private final String path;

    HttpInvokerHostService(
            final Supplier<Host> host,
            final Supplier<HttpAuthenticationFactory> httpAuthenticationFactory,
            final Supplier<SecurityRealm> realmService,
            final Supplier<PathHandler> remoteHttpInvokerService,
            final String path) {
        this.host = host;
        this.httpAuthenticationFactory = httpAuthenticationFactory;
        this.realmService = realmService;
        this.remoteHttpInvokerService = remoteHttpInvokerService;
        this.path = path;
    }

    @Override
    public void start(final StartContext startContext) {
        HttpHandler handler = remoteHttpInvokerService.get();
        if (httpAuthenticationFactory != null) {
            handler = secureAccess(handler, httpAuthenticationFactory.get());
        } else if(realmService != null) {
            handler = secureAccess(handler, realmService.get().getHttpAuthenticationFactory());
        }
        handler = setupRoutes(handler);
        host.get().registerHandler(path, handler);
        host.get().registerLocation(path);
    }

    @Override
    public void stop(final StopContext stopContext) {
        host.get().unregisterHandler(path);
        host.get().unregisterLocation(path);
    }

    private HttpHandler setupRoutes(HttpHandler handler) {
        final SimpleSessionIdentifierCodec codec = new SimpleSessionIdentifierCodec(new SimpleRoutingSupport(), this.host.get().getServer().getRoute());
        final SecureRandomSessionIdGenerator generator = new SecureRandomSessionIdGenerator();
        return exchange -> {
            exchange.addResponseCommitListener(ex -> {
                Cookie cookie = ex.getResponseCookies().get(JSESSIONID);
                if (cookie != null ) {
                    cookie.setValue(codec.encode(cookie.getValue()).toString());
                } else if (ex.getStatusCode() == StatusCodes.UNAUTHORIZED) {
                    // add a session cookie in order to avoid sticky session issue after 401 Unauthorized response
                    cookie = new CookieImpl("JSESSIONID", codec.encode(generator.createSessionId()).toString());
                    cookie.setPath(ex.getResolvedPath());
                    exchange.getResponseCookies().put("JSESSIONID", cookie);
                }
            });
            handler.handleRequest(exchange);
        };
    }

    private static HttpHandler secureAccess(HttpHandler domainHandler, final HttpAuthenticationFactory httpAuthenticationFactory) {
        domainHandler = new AuthenticationCallHandler(domainHandler);
        domainHandler = new AuthenticationConstraintHandler(domainHandler);
        Supplier<List<HttpServerAuthenticationMechanism>> mechanismSupplier = () ->
                httpAuthenticationFactory.getMechanismNames().stream()
                        .map(s -> {
                            try {
                                return httpAuthenticationFactory.createMechanism(s);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .collect(Collectors.toList());
        domainHandler = ElytronContextAssociationHandler.builder()
                .setNext(domainHandler)
                .setMechanismSupplier(mechanismSupplier)
                .setHttpExchangeSupplier(h -> new ElytronHttpExchange(h) {

                    @Override
                    public void authenticationComplete(SecurityIdentity securityIdentity, String mechanismName) {
                        super.authenticationComplete(securityIdentity, mechanismName);
                        h.putAttachment(ElytronIdentityHandler.IDENTITY_KEY, securityIdentity);
                    }

                })
                .build();

        return domainHandler;
    }

    public String getPath() {
        return path;
    }
}

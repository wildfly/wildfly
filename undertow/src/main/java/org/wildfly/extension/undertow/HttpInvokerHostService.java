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

import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.as.web.session.SimpleSessionIdentifierCodec;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.elytron.web.undertow.server.ElytronContextAssociationHandler;
import org.wildfly.elytron.web.undertow.server.ElytronHttpExchange;
import org.wildfly.httpclient.common.ElytronIdentityHandler;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.PathHandler;

/**
 * @author Stuart Douglas
 */
class HttpInvokerHostService implements Service<HttpInvokerHostService> {

    private static final String JSESSIONID = "JSESSIONID";

    private final String path;
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final InjectedValue<HttpAuthenticationFactory> httpAuthenticationFactoryInjectedValue = new InjectedValue<>();
    private final InjectedValue<SecurityRealmService> realmService = new InjectedValue<>();
    private final InjectedValue<PathHandler> remoteHttpInvokerServiceInjectedValue = new InjectedValue<>();

    public HttpInvokerHostService(String path) {
        this.path = path;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        HttpHandler handler = remoteHttpInvokerServiceInjectedValue.getValue();
        if(httpAuthenticationFactoryInjectedValue.getOptionalValue() != null) {
            handler = secureAccess(handler, httpAuthenticationFactoryInjectedValue.getOptionalValue());
        } else if(realmService.getOptionalValue() != null) {
            handler = secureAccess(handler, realmService.getOptionalValue().getHttpAuthenticationFactory());
        }
        handler = setupRoutes(handler);
        host.getValue().registerHandler(path, handler);
        host.getValue().registerLocation(path);
    }

    @Override
    public void stop(StopContext stopContext) {
        host.getValue().unregisterHandler(path);
        host.getValue().unregisterLocation(path);
    }

    private HttpHandler setupRoutes(HttpHandler handler) {
        final SimpleSessionIdentifierCodec codec = new SimpleSessionIdentifierCodec(new SimpleRoutingSupport(), this.host.getValue().getServer().getRoute());
        return exchange -> {
            exchange.addResponseCommitListener(ex -> {
                Cookie cookie = ex.getResponseCookies().get(JSESSIONID);
                if(cookie != null ) {
                    cookie.setValue(codec.encode(cookie.getValue()));
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

    @Override
    public HttpInvokerHostService getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public String getPath() {
        return path;
    }

    public InjectedValue<Host> getHost() {
        return host;
    }

    public InjectedValue<HttpAuthenticationFactory> getHttpAuthenticationFactoryInjectedValue() {
        return httpAuthenticationFactoryInjectedValue;
    }

    public InjectedValue<PathHandler> getRemoteHttpInvokerServiceInjectedValue() {
        return remoteHttpInvokerServiceInjectedValue;
    }

    public InjectedValue<SecurityRealmService> getRealmService() {
        return realmService;
    }
}

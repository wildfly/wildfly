/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.servlet.api.SessionConfigWrapper;
import io.undertow.util.StatusCodes;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.elytron.web.undertow.server.ElytronContextAssociationHandler;
import org.wildfly.elytron.web.undertow.server.ElytronHttpExchange;
import org.wildfly.extension.undertow.session.AffinitySessionConfigWrapper;
import org.wildfly.extension.undertow.session.AffinitySessionIdentifierCodec;
import org.wildfly.extension.undertow.session.CodecSessionConfigWrapper;
import org.wildfly.extension.undertow.session.NonDistributableSessionAffinityProvider;
import org.wildfly.extension.undertow.session.SessionAffinityProvider;
import org.wildfly.httpclient.common.ElytronIdentityHandler;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HttpInvokerHostService implements Service {

    private final Supplier<Host> host;
    private final Supplier<HttpAuthenticationFactory> httpAuthenticationFactory;
    private final Supplier<PathHandler> remoteHttpInvokerService;
    private final String path;

    HttpInvokerHostService(
            final Supplier<Host> host,
            final Supplier<HttpAuthenticationFactory> httpAuthenticationFactory,
            final Supplier<PathHandler> remoteHttpInvokerService,
            final String path) {
        this.host = host;
        this.httpAuthenticationFactory = httpAuthenticationFactory;
        this.remoteHttpInvokerService = remoteHttpInvokerService;
        this.path = path;
    }

    @Override
    public void start(final StartContext startContext) {
        HttpHandler handler = remoteHttpInvokerService.get();
        if (httpAuthenticationFactory != null) {
            handler = secureAccess(handler, httpAuthenticationFactory.get());
        }

        SessionCookieConfig sessionConfig = new SessionCookieConfig();
        sessionConfig.setPath(this.path);
        Server server = this.host.get().getServer();
        ServletContainerService container = server.getServletContainer();
        CookieConfig affinityCookeConfig = container.getAffinityCookieConfig();
        CookieConfig sessionCookieConfig = container.getSessionCookieConfig();
        if (sessionCookieConfig != null) {
            if (sessionCookieConfig.getDomain() != null) {
                sessionConfig.setDomain(sessionCookieConfig.getDomain());
            }
            if (sessionCookieConfig.getMaxAge() != null) {
                sessionConfig.setMaxAge(sessionCookieConfig.getMaxAge());
            }
            if (sessionCookieConfig.getSecure() != null) {
                sessionConfig.setSecure(sessionCookieConfig.getSecure());
            }
            if (sessionCookieConfig.getHttpOnly() != null) {
                sessionConfig.setHttpOnly(sessionCookieConfig.getHttpOnly());
            }
        }
        SessionAffinityProvider affinityProvider = new NonDistributableSessionAffinityProvider(server);
        SessionConfigWrapper wrapper = (affinityCookeConfig != null) ? new AffinitySessionConfigWrapper(affinityCookeConfig, affinityProvider) : new CodecSessionConfigWrapper(new AffinitySessionIdentifierCodec(affinityProvider));

        handler = setupRoutes(handler, wrapper.wrap(sessionConfig, null));
        host.get().registerHandler(path, handler);
        host.get().registerLocation(path);
    }

    @Override
    public void stop(final StopContext stopContext) {
        host.get().unregisterHandler(path);
        host.get().unregisterLocation(path);
    }

    private HttpHandler setupRoutes(HttpHandler handler, SessionConfig sessionConfig) {
        final SecureRandomSessionIdGenerator generator = new SecureRandomSessionIdGenerator();
        return exchange -> {
            exchange.addResponseCommitListener(ex -> {
                String id = sessionConfig.findSessionId(exchange);
                if (id != null) {
                    sessionConfig.setSessionId(exchange, id);
                } else if (ex.getStatusCode() == StatusCodes.UNAUTHORIZED) {
                    // add a session cookie in order to avoid sticky session issue after 401 Unauthorized response
                    sessionConfig.setSessionId(exchange, generator.createSessionId());
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

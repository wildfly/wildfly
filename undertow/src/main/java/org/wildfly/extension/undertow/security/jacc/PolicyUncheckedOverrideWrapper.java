/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.security.jacc;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import jakarta.security.jacc.WebResourcePermission;
import org.jboss.logging.Logger;
import org.wildfly.common.Assert;
import org.wildfly.elytron.web.undertow.server.SecurityContextImpl;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.security.authz.jacc.UncheckedPolicyUtil;

/**
 * Handler wrapper that overrides the authentication requirement when Jakarta Authorization Policy
 * indicates a resource is unchecked (accessible to unauthenticated callers).
 *
 * This wrapper executes AFTER ServletAuthenticationConstraintHandler sets the authentication
 * required flag, allowing Policy to override constraint-based requirements.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PolicyUncheckedOverrideWrapper implements HandlerWrapper {

    private static final Logger log = Logger.getLogger(PolicyUncheckedOverrideWrapper.class);

    private final UncheckedPolicyUtil policyUtil;

    public PolicyUncheckedOverrideWrapper(UncheckedPolicyUtil policyUtil) {
        this.policyUtil = Assert.checkNotNullParam("policyUtil", policyUtil);
    }

    @Override
    public HttpHandler wrap(HttpHandler next) {
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                SecurityContext securityContext = exchange.getSecurityContext();

                if (securityContext instanceof SecurityContextImpl) {
                    SecurityContextImpl impl = (SecurityContextImpl) securityContext;
                    WebResourcePermission permission = createWebResourcePermission(exchange);

                    if (log.isTraceEnabled()) {
                        log.tracef("Checking Policy for permission: %s", permission);
                    }

                    boolean isUnchecked = policyUtil.isUnchecked(permission);

                    // Only clear authentication requirement if Policy says resource is unchecked
                    // Don't force authentication if it's not unchecked - let constraint handler decision stand
                    if (isUnchecked) {
                        impl.setAuthenticationRequired(false);

                        if (log.isTraceEnabled()) {
                            log.tracef("Policy indicates resource is unchecked, clearing authentication requirement for: %s %s",
                                    exchange.getRequestMethod(), exchange.getRequestPath());
                        }
                    } else {
                        if (log.isTraceEnabled()) {
                            log.tracef("Policy indicates resource is checked, leaving authentication requirement unchanged for: %s %s",
                                    exchange.getRequestMethod(), exchange.getRequestPath());
                        }
                    }
                } else if (securityContext != null) {
                    UndertowLogger.ROOT_LOGGER.unexpectedSecurityContextType(securityContext.getClass().getName());
                }

                next.handleRequest(exchange);
            }
        };
    }

    /**
     * Create a WebResourcePermission for the current request.
     *
     * @param exchange the HTTP exchange
     * @return the permission representing this request
     */
    private WebResourcePermission createWebResourcePermission(HttpServerExchange exchange) {
        // Use relative path (without context path) to match how Jakarta Authorization
        // creates permissions from web.xml security constraints
        String relativePath = exchange.getRelativePath();
        String httpMethod = exchange.getRequestMethod().toString();

        // WebResourcePermission format: WebResourcePermission(String name, String actions)
        // name = request path relative to context (servlet mapping)
        // actions = HTTP method (or null for all methods)
        return new WebResourcePermission(relativePath, httpMethod);
    }
}

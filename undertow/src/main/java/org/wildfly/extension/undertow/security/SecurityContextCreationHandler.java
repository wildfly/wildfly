/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow.security;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.security.SecurityContext;

/**
 * Handler that creates the security context and attaches it to the exchange. It is not associated with the current
 * thread at this point, as request may not have been dispatched into the final thread that will actually run the
 * servlet request.
 *
 * @author Stuart Douglas
 */
public class SecurityContextCreationHandler implements HttpHandler {

    private final String securityDomain;
    private final HttpHandler next;

    public SecurityContextCreationHandler(final String securityDomain, final HttpHandler next) {
        this.securityDomain = securityDomain;
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        try {
            final SecurityContext sc = SecurityActions.createSecurityContext(securityDomain);
            exchange.putAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT, sc);
            SecurityActions.setSecurityContextOnAssociation(sc);

            next.handleRequest(exchange);

        } finally {
            SecurityActions.clearSecurityContext();
        }

    }

    public static final HandlerWrapper wrapper(final String securityDomain) {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new SecurityContextCreationHandler(securityDomain, handler);
            }
        };
    }
}

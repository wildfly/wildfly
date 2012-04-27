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

package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.TEMPORARY_REDIRECT;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.IOException;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Filter;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;

/**
 * Filter to redirect to the error context while the security realm is not ready.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class RealmReadinessFilter extends Filter {

    private final SecurityRealm securityRealm;
    private final String redirectTo;

    RealmReadinessFilter(final SecurityRealm securityRealm, final String redirectTo) {
        this.securityRealm = securityRealm;
        this.redirectTo = redirectTo;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (securityRealm.isReady()) {
            chain.doFilter(exchange);
        } else {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add(LOCATION, redirectTo);
            exchange.sendResponseHeaders(TEMPORARY_REDIRECT, 0);
            exchange.close();
        }
    }

    @Override
    public String description() {
        return MESSAGES.realmReadinessFilter();
    }

}

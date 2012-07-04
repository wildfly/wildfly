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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;
import static org.jboss.as.domain.http.server.Constants.HOST;
import static org.jboss.as.domain.http.server.Constants.HTTP;
import static org.jboss.as.domain.http.server.Constants.HTTPS;
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.IOException;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpsServer;
import org.jboss.dmr.ModelNode;

/**
 * A RealmReadinessFilter implementation to report by DMR failure that requests can not be processed as the realm is not ready.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DmrFailureReadinessFilter extends RealmReadinessFilter {

    private final String redirectTo;

    DmrFailureReadinessFilter(final SecurityRealm securityRealm, final String redirectTo) {
        super(securityRealm);
        this.redirectTo = redirectTo;
    }

    /**
     * @see org.jboss.as.domain.http.server.RealmReadinessFilter#rejectRequest(org.jboss.com.sun.net.httpserver.HttpExchange)
     */
    @Override
    void rejectRequest(HttpExchange exchange) throws IOException {
        final Headers headers = exchange.getRequestHeaders();
        String host = headers.getFirst(HOST);
        String protocol = exchange.getHttpContext().getServer() instanceof HttpsServer ? HTTPS : HTTP;
        String redirectUrl = protocol + "://" + host + redirectTo;

        ModelNode rejection = new ModelNode();
        rejection.get(OUTCOME).set(FAILED);
        rejection.get(FAILURE_DESCRIPTION).set(MESSAGES.realmNotReadyMessage(redirectUrl));
        rejection.get(ROLLED_BACK).set(Boolean.TRUE.toString());

        // Keep the response visible so it can easily be seen in network traces.
        writeResponse(exchange, false, true, rejection, FORBIDDEN, false);
    }

    /**
     * @see org.jboss.com.sun.net.httpserver.Filter#description()
     */
    @Override
    public String description() {
        return MESSAGES.dmrFailureReadinessFilter();
    }

}

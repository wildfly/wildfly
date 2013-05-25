/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.domain.http.server.DomainUtil.constructUrl;
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;

import io.undertow.util.Methods;
import org.jboss.as.domain.http.server.OperationParameter;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.dmr.ModelNode;

/**
 * A RealmReadinessFilter implementation to report by DMR failure that requests can not be processed as the realm is not ready.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DmrFailureReadinessHandler extends RealmReadinessHandler {

    private final String redirectTo;

    public DmrFailureReadinessHandler(final SecurityRealm securityRealm, final HttpHandler next, final String redirectTo) {
        super(securityRealm, next);
        this.redirectTo = redirectTo;
    }

    /**
     * @see org.jboss.as.domain.http.server.security.RealmReadinessHandler#rejectRequest(io.undertow.server.HttpServerExchange)
     */
    @Override
    void rejectRequest(HttpServerExchange exchange) throws IOException {
        ModelNode rejection = new ModelNode();
        rejection.get(OUTCOME).set(FAILED);
        rejection.get(FAILURE_DESCRIPTION).set(MESSAGES.realmNotReadyMessage(constructUrl(exchange, redirectTo)));
        rejection.get(ROLLED_BACK).set(Boolean.TRUE.toString());

        // Keep the response visible so it can easily be seen in network traces.
        boolean get = exchange.getRequestMethod().equals(Methods.GET);
        OperationParameter operationParameter = new OperationParameter.Builder(get)
                .encode(false)
                .pretty(true)
                .build();
        writeResponse(exchange, 403, rejection, operationParameter);
    }
}
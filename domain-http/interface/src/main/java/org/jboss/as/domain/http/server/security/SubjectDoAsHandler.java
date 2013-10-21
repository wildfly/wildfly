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

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.jboss.as.controller.AccessAuditContext;

/**
 * HttpHandler to ensure the Subject for the current authenticated user is correctly associated for the request.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SubjectDoAsHandler implements HttpHandler {

    private final HttpHandler wrapped;

    public SubjectDoAsHandler(final HttpHandler toWrap) {
        this.wrapped = toWrap;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final SecurityContext securityContext = exchange.getAttachment(SecurityContext.ATTACHMENT_KEY);
        Subject useSubject = null;
        if (securityContext != null) {
            final Account account = securityContext.getAuthenticatedAccount();
            if (account instanceof SubjectAccount) {
                useSubject = ((SubjectAccount) account).getSubject();
            }
        }
        handleRequest(exchange, useSubject);
    }

    void handleRequest(final HttpServerExchange exchange, final Subject subject) throws Exception {
        if (subject != null) {
            try {
                AccessAuditContext.doAs(subject, new PrivilegedExceptionAction<Void>() {

                    @Override
                    public Void run() throws Exception {
                        wrapped.handleRequest(exchange);
                        return null;
                    }

                });
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        } else {
            wrapped.handleRequest(exchange);
        }
    }

}

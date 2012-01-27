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

import java.io.IOException;

import javax.security.auth.Subject;

import org.jboss.as.controller.security.SecurityContext;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpExchange.AttributeScope;
import org.jboss.com.sun.net.httpserver.HttpHandler;

/**
 * Handler to ensure that the Subject for the authenticated user is associated for this request.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SubjectAssociationHandler implements HttpHandler {

    private final HttpHandler wrapped;

    public SubjectAssociationHandler(HttpHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Subject subject = (Subject) exchange.getAttribute(Subject.class.getName(), AttributeScope.CONNECTION);
        SecurityContext.setSubject(subject);
        try {
            wrapped.handle(exchange);
        } finally {
            SecurityContext.clearSubject();
        }
    }

}

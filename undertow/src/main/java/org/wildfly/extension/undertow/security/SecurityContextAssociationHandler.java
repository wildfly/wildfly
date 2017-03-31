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

package org.wildfly.extension.undertow.security;

import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.servlet.handlers.ServletChain;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.predicate.DispatcherTypePredicate;
import org.jboss.metadata.javaee.jboss.RunAsIdentityMetaData;
import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.servlet.ServletRequest;

public class SecurityContextAssociationHandler implements HttpHandler {

    private final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap;
    private final HttpHandler next;

    private static final PrivilegedAction<ServletRequestContext> CURRENT_CONTEXT = new PrivilegedAction<ServletRequestContext>() {
        @Override
        public ServletRequestContext run() {
            return ServletRequestContext.current();
        }
    };

    public SecurityContextAssociationHandler(final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap, final HttpHandler next) {
        this.runAsIdentityMetaDataMap = runAsIdentityMetaDataMap;
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        SecurityContext sc = exchange.getAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT);
        RunAsIdentityMetaData identity = null;
        RunAs old = null;
        try {
            final ServletChain servlet = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getCurrentServlet();
            identity = runAsIdentityMetaDataMap.get(servlet.getManagedServlet().getServletInfo().getName());
            RunAsIdentity runAsIdentity = null;
            if (identity != null) {
                UndertowLogger.ROOT_LOGGER.tracef("%s, runAs: %s", servlet.getManagedServlet().getServletInfo().getName(), identity);
                runAsIdentity = new RunAsIdentity(identity.getRoleName(), identity.getPrincipalName(), identity.getRunAsRoles());
            }
            old = SecurityActions.setRunAsIdentity(runAsIdentity, sc);

            // Perform the request
            next.handleRequest(exchange);
        } finally {
            if (identity != null) {
                SecurityActions.setRunAsIdentity(old, sc);
            }
        }
    }

    public static HandlerWrapper wrapper(final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap) {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                //we only run this on REQUEST or ASYNC invocations
                return new PredicateHandler(Predicates.or(DispatcherTypePredicate.REQUEST, DispatcherTypePredicate.ASYNC), new SecurityContextAssociationHandler(runAsIdentityMetaDataMap, handler), handler);
            }
        };
    }

    public static ServletRequest getActiveRequest() {
        ServletRequestContext current;
        if(System.getSecurityManager() == null) {
            current = ServletRequestContext.current();
        } else {
            current = AccessController.doPrivileged(CURRENT_CONTEXT);
        }
        if(current == null) {
            return null;
        }
        return current.getServletRequest();
    }
}

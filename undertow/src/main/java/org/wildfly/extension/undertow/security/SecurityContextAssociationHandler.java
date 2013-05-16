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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import javax.security.jacc.PolicyContext;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletChain;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.metadata.javaee.jboss.RunAsIdentityMetaData;
import org.wildfly.extension.undertow.UndertowLogger;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityRolesAssociation;

public class SecurityContextAssociationHandler implements HttpHandler {

    private final Map<String, Set<String>> principleVsRoleMap;
    private final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap;
    private final String contextId;
    private final HttpHandler next;

    public SecurityContextAssociationHandler(final Map<String, Set<String>> principleVsRoleMap, final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap, final String contextId, final HttpHandler next) {
        this.principleVsRoleMap = principleVsRoleMap;
        this.runAsIdentityMetaDataMap = runAsIdentityMetaDataMap;
        this.contextId = contextId;
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        SecurityContext sc = exchange.getAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT);
        String previousContextID = null;
        RunAsIdentityMetaData identity = null;
        try {
            SecurityActions.setSecurityContextOnAssociation(sc);
            ServletChain servlet = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getCurrentServlet();
            identity = runAsIdentityMetaDataMap.get(servlet.getManagedServlet().getServletInfo().getName());
            RunAsIdentity runAsIdentity = null;
            if (identity != null) {
                UndertowLogger.ROOT_LOGGER.tracef("%s, runAs: %s", servlet.getManagedServlet().getServletInfo().getName(), identity);
                runAsIdentity = new RunAsIdentity(identity.getRoleName(), identity.getPrincipalName(), identity.getRunAsRoles());
            }
            SecurityActions.pushRunAsIdentity(runAsIdentity);

            // set JACC contextID
            previousContextID = setContextID(contextId);

            // Perform the request
            next.handleRequest(exchange);
        } finally {
            if (identity != null) {
                SecurityActions.popRunAsIdentity();
            }
            SecurityActions.clearSecurityContext();
            SecurityRolesAssociation.setSecurityRoles(null);
            setContextID(previousContextID);
        }
    }

    private static class SetContextIDAction implements PrivilegedAction<String> {

        private String contextID;

        SetContextIDAction(String contextID) {
            this.contextID = contextID;
        }

        @Override
        public String run() {
            String currentContextID = PolicyContext.getContextID();
            PolicyContext.setContextID(this.contextID);
            return currentContextID;
        }
    }

    private String setContextID(String contextID) {
        PrivilegedAction<String> action = new SetContextIDAction(contextID);
        return AccessController.doPrivileged(action);
    }


    public static HandlerWrapper wrapper(final Map<String, Set<String>> principleVsRoleMap, final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap, final String contextId) {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new SecurityContextAssociationHandler(principleVsRoleMap, runAsIdentityMetaDataMap, contextId, handler);
            }
        };
    }
}

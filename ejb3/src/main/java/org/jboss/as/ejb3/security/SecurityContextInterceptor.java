/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.security;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.ejb.EJBAccessException;
import javax.security.jacc.PolicyContext;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.security.SecurityRolesAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;

/**
 * Establish the security context.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Anil Saldhana
 */
public class SecurityContextInterceptor implements Interceptor {
    private final PrivilegedAction<Void> pushAction;
    private final PrivilegedAction<Void> popAction;
    private final String policyContextID;

    public SecurityContextInterceptor(final SecurityContextInterceptorHolder holder) {
        this.pushAction = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                holder.securityManager.push(holder.securityDomain);
                try {
                    if (holder.skipAuthentication == false) {
                        holder.securityManager.authenticate(holder.runAs, holder.runAsPrincipal, holder.extraRoles);
                    }
                    if (holder.principalVsRolesMap != null) {
                        SecurityRolesAssociation.setSecurityRoles(holder.principalVsRolesMap);
                    }
                } catch (Throwable t) {
                    // undo the push actions on failure
                    if (holder.principalVsRolesMap != null) {
                        // clear the threadlocal
                        SecurityRolesAssociation.setSecurityRoles(null);
                    }
                    holder.securityManager.pop();

                    if (t instanceof SecurityException) {
                        throw new EJBAccessException(t.getMessage());
                    }
                    throw t;
                }
                return null;
            }
        };
        this.popAction = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (holder.principalVsRolesMap != null) {
                    // Clear the threadlocal
                    SecurityRolesAssociation.setSecurityRoles(null);
                }
                holder.securityManager.pop();
                return null;
            }
        };
        this.policyContextID = holder.policyContextID;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        // TODO - special cases need to be handled where SecurityContext not established or minimal unauthenticated principal context instead.
        String previousContextID = this.setContextID(this.policyContextID);
        if (WildFlySecurityManager.isChecking()) {
            doPrivileged(pushAction);
        } else {
            pushAction.run();
        }
        try {
            return context.proceed();
        } finally {
            this.setContextID(previousContextID);
            if (WildFlySecurityManager.isChecking()) {
                doPrivileged(popAction);
            } else {
                popAction.run();
            }
        }
    }

    /**
     * <p>
     * Sets the JACC contextID using a privileged action and returns the previousID from the {@code PolicyContext}.
     * </p>
     *
     * @param contextID the JACC contextID to be set.
     * @return the previous contextID as retrieved from the {@code PolicyContext}.
     */
    protected String setContextID(final String contextID) {
        if (! WildFlySecurityManager.isChecking()) {
            final String previousID = PolicyContext.getContextID();
            PolicyContext.setContextID(contextID);
            return previousID;
        } else {
            final PrivilegedAction<String> action = new SetContextIDAction(contextID);
            return AccessController.doPrivileged(action);
        }
    }

    /**
     * PrivilegedAction that sets the {@code PolicyContext} id.
     */
    private static class SetContextIDAction implements PrivilegedAction<String> {

        private String contextID;

        SetContextIDAction(final String contextID) {
            this.contextID = contextID;
        }

        @Override
        public String run() {
            final String previousID = PolicyContext.getContextID();
            PolicyContext.setContextID(this.contextID);
            return previousID;
        }
    }
}

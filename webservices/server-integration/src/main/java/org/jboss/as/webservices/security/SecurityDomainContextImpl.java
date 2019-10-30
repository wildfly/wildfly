/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.security;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.security.auth.Subject;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;

/**
 * Adaptor of org.jboss.as.security.plugins.SecurityDomainContext to org.jboss.wsf.spi.security.SecurityDomainContext
 *
 * @author alessio.soldano@jboss.com
 * @since 13-May-2011
 */
public final class SecurityDomainContextImpl implements org.jboss.wsf.spi.security.SecurityDomainContext {

    private final SecurityDomainContext context;

    public SecurityDomainContextImpl(SecurityDomainContext context) {
        this.context = context;
    }

    @Override
    public boolean isValid(Principal principal, Object credential, Subject activeSubject) {
        return context.getAuthenticationManager().isValid(principal, credential, activeSubject);
    }

    @Override
    public boolean doesUserHaveRole(Principal principal, Set<Principal> roles) {
        return context.getAuthorizationManager().doesUserHaveRole(principal, roles);
    }

    @Override
    public String getSecurityDomain() {
        return context.getAuthenticationManager().getSecurityDomain();
    }

    @Override
    public Set<Principal> getUserRoles(Principal principal) {
        return context.getAuthorizationManager().getUserRoles(principal);
    }

    @Override
    public void pushSubjectContext(final Subject subject, final Principal principal, final Object credential) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
                if (securityContext == null) {
                    securityContext = createSecurityContext(getSecurityDomain());
                    setSecurityContextOnAssociation(securityContext);
                }
                securityContext.getUtil().createSubjectInfo(principal, credential, subject);
                return null;
            }
        });
    }

    /**
     * Create a JBoss Security Context with the given security domain name
     *
     * @param domain the security domain name (such as "other" )
     * @return an instanceof {@code SecurityContext}
     */
    private static SecurityContext createSecurityContext(final String domain) {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

            @Override
            public SecurityContext run() {
                try {
                    return SecurityContextFactory.createSecurityContext(domain);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Set the {@code SecurityContext} on the {@code SecurityContextAssociation}
     *
     * @param sc the security context
     */
    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    //subject will be pushed in thread local context, so directly run this action
    public void runAs(Callable<Void> action) throws Exception {
        action.call();
    }
}
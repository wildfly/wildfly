/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security;

import static org.jboss.as.web.WebMessages.MESSAGES;

import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged Actions
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 12, 2011
 */
class SecurityActions {

    /**
     * Create a JBoss Security Context with the given security domain name
     *
     * @param domain the security domain name (such as "other" )
     * @return an instanceof {@code SecurityContext}
     */
    static SecurityContext createSecurityContext(final String domain) {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

            @Override
            public SecurityContext run() {
                try {
                    return SecurityContextFactory.createSecurityContext(domain);
                } catch (Exception e) {
                    throw MESSAGES.failToCreateSecurityContext(e);
                }
            }
        });
    }

    /**
     * Set the {@code SecurityContext} on the {@code SecurityContextAssociation}
     *
     * @param sc the security context
     */
    static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    /**
     * Get the current {@code SecurityContext}
     *
     * @return an instance of {@code SecurityContext}
     */
    static SecurityContext getSecurityContext() {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        });
    }

    /**
     * Clears current {@code SecurityContext}
     */
    static void clearSecurityContext() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                SecurityContextAssociation.clearSecurityContext();
                return null;
            }
        });
    }

    /**
     * Sets the run as identity
     *
     * @param principal the identity
     */
    static void pushRunAsIdentity(final RunAsIdentity principal) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContext sc = getSecurityContext();
                if (sc == null)
                    throw MESSAGES.noSecurityContext();
                sc.setOutgoingRunAs(principal);
                return null;
            }
        });
    }

    /**
     * Removes the run as identity
     *
     * @return the identity removed
     */
    static RunAs popRunAsIdentity() {
        return AccessController.doPrivileged(new PrivilegedAction<RunAs>() {

            @Override
            public RunAs run() {
                SecurityContext sc = getSecurityContext();
                if (sc == null)
                    throw MESSAGES.noSecurityContext();
                RunAs principal = sc.getOutgoingRunAs();
                sc.setOutgoingRunAs(null);
                return principal;
            }
        });
    }

    public static final String AUTH_EXCEPTION_KEY = "org.jboss.security.exception";

    static void clearAuthException() {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    SecurityContext sc = getSecurityContext();
                    if (sc != null)
                        sc.getData().put(AUTH_EXCEPTION_KEY, null);
                    return null;
                }
            });
        } else {
            SecurityContext sc = getSecurityContext();
            if (sc != null)
                sc.getData().put(AUTH_EXCEPTION_KEY, null);
        }
    }

    static Throwable getAuthException() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Throwable>() {

                @Override
                public Throwable run() {
                    SecurityContext sc = getSecurityContext();
                    Throwable exception = null;
                    if (sc != null)
                        exception = (Throwable) sc.getData().get(AUTH_EXCEPTION_KEY);
                    return exception;
                }
            });
        } else {
            SecurityContext sc = getSecurityContext();
            Throwable exception = null;
            if (sc != null)
                exception = (Throwable) sc.getData().get(AUTH_EXCEPTION_KEY);
            return exception;
        }
    }
}
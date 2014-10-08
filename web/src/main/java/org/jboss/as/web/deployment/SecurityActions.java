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

package org.jboss.as.web.deployment;

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;

/**
 * Privileged actions used by this package.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jan-2010
 */
class SecurityActions {

    private SecurityActions() {
    }

    static final PrivilegedAction<SecurityContext> GET_SECURITY_CONTEXT = new PrivilegedAction<SecurityContext>() {
        public SecurityContext run() {
            return SecurityContextAssociation.getSecurityContext();
        }
    };

    public static final PrivilegedAction<Void> CLEAR_SECURITY_CONTEXT = new PrivilegedAction<Void>() {
        public Void run() {
            SecurityContextAssociation.clearSecurityContext();
            return null;
        }
    };

    static String getSystemProperty(final String key, final String defaultValue) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key, defaultValue);
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(key, defaultValue);
            }
        });
    }

    static void setSystemProperty(final String key, final String value) {
        if (System.getSecurityManager() == null) {
            System.setProperty(key, value);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    System.setProperty(key, value);
                    return null;
                }
            });
        }
    }

    /**
     * Create a JBoss Security Context with the given security domain name
     *
     * @param domain the security domain name (such as "other" )
     * @return an instanceof {@code SecurityContext}
     */
    static SecurityContext createSecurityContext(final String domain) {
        if (System.getSecurityManager() == null) {
            try {
                return SecurityContextFactory.createSecurityContext(domain);
            } catch (Exception e) {
                throw MESSAGES.failToCreateSecurityContext(e);
            }
        } else {
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
    }

    /**
     * Set the {@code SecurityContext} on the {@code SecurityContextAssociation}
     *
     * @param sc the security context
     */
    static void setSecurityContextOnAssociation(final SecurityContext sc) {
        if (System.getSecurityManager() == null) {
            SecurityContextAssociation.setSecurityContext(sc);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    SecurityContextAssociation.setSecurityContext(sc);
                    return null;
                }
            });
        }
    }

    /**
     * Get the current {@code SecurityContext}
     *
     * @return an instance of {@code SecurityContext}
     */
    static SecurityContext getSecurityContext() {
        if (System.getSecurityManager() == null) {
            return SecurityContextAssociation.getSecurityContext();
        } else {
            return AccessController.doPrivileged(GET_SECURITY_CONTEXT);
        }
    }

    /**
     * Sets the run as identity
     *
     * @param principal the identity
     */
    static void pushRunAsIdentity(final RunAsIdentity principal, final SecurityContext sc) {
        if (sc == null)
            throw MESSAGES.noSecurityContext();
        if (System.getSecurityManager() == null) {
            sc.setOutgoingRunAs(principal);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    sc.setOutgoingRunAs(principal);
                    return null;
                }
            });
        }
    }

    /**
     * Clears current {@code SecurityContext}
     */
    static void clearSecurityContext() {
        if (System.getSecurityManager() == null) {
            SecurityContextAssociation.clearSecurityContext();
        } else {
            AccessController.doPrivileged(CLEAR_SECURITY_CONTEXT);
        }
    }
}

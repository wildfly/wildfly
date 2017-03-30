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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import org.jboss.security.RunAs;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SecurityRolesAssociation;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.security.auth.Subject;

import static java.security.AccessController.doPrivileged;

/**
 * Privileged Actions
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 12, 2011
 */
class SecurityActions {

    public static final String AUTH_EXCEPTION_KEY = "org.jboss.security.exception";

    /**
     * Create a JBoss Security Context with the given security domain name
     *
     * @param domain the security domain name (such as "other" )
     * @return an instanceof {@code SecurityContext}
     */
    static SecurityContext createSecurityContext(final String domain) {
        if (WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<SecurityContext>() {
                @Override
                public SecurityContext run() {
                    try {
                        return SecurityContextFactory.createSecurityContext(domain);
                    } catch (Exception e) {
                        throw UndertowLogger.ROOT_LOGGER.failToCreateSecurityContext(e);
                    }
                }
            });
        } else {
            try {
                return SecurityContextFactory.createSecurityContext(domain);
            } catch (Exception e) {
                throw UndertowLogger.ROOT_LOGGER.failToCreateSecurityContext(e);
            }
        }
    }

    /**
     * Set the {@code SecurityContext} on the {@code SecurityContextAssociation}
     *
     * @param sc the security context
     */
    static void setSecurityContextOnAssociation(final SecurityContext sc) {
        if (WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    SecurityContextAssociation.setSecurityContext(sc);
                    return null;
                }
            });
        } else {
            SecurityContextAssociation.setSecurityContext(sc);
        }
    }

    /**
     * Get the current {@code SecurityContext}
     *
     * @return an instance of {@code SecurityContext}
     */
    static SecurityContext getSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<SecurityContext>() {
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        } else {
            return SecurityContextAssociation.getSecurityContext();
        }
    }

    /**
     * Clears current {@code SecurityContext}
     */
    static void clearSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
                public Void run() {
                    SecurityContextAssociation.clearSecurityContext();
                    return null;
                }
            });
        } else {
            SecurityContextAssociation.clearSecurityContext();
        }
    }

    static void setSecurityRoles(final Map<String, Set<String>> roles) {
        if(WildFlySecurityManager.isChecking()) {

            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
                public Void run() {
                    SecurityRolesAssociation.setSecurityRoles(roles);
                    return null;
                }
            });
        } else {
            SecurityRolesAssociation.setSecurityRoles(roles);
        }
    }

    /**
     * Sets the run as identity
     *
     * @param principal the identity
     */
    static RunAs setRunAsIdentity(final RunAs principal, final SecurityContext sc) {
        if (WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<RunAs>() {

                @Override
                public RunAs run() {
                    if (sc == null) {
                        throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
                    }
                    RunAs old = sc.getOutgoingRunAs();
                    sc.setOutgoingRunAs(principal);
                    return old;
                }
            });
        } else {
            if (sc == null) {
                throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
            }
            RunAs old = sc.getOutgoingRunAs();
            sc.setOutgoingRunAs(principal);
            return old;
        }
    }

    /**
     * Removes the run as identity
     *
     * @return the identity removed
     */
    static RunAs popRunAsIdentity(final SecurityContext sc) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<RunAs>() {
                @Override
                public RunAs run() {
                    if (sc == null) {
                        throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
                    }
                    RunAs principal = sc.getOutgoingRunAs();
                    sc.setOutgoingRunAs(null);
                    return principal;
                }
            });
        } else {
            if (sc == null) {
                throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
            }
            RunAs principal = sc.getOutgoingRunAs();
            sc.setOutgoingRunAs(null);
            return principal;
        }
    }

    public static RunAs getRunAsIdentity(final SecurityContext sc) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<RunAs>() {
                @Override
                public RunAs run() {
                    if (sc == null) {
                        throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
                    }
                    return sc.getOutgoingRunAs();
                }
            });
        } else {
            if (sc == null) {
                throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
            }
            return sc.getOutgoingRunAs();
        }
    }
    static Subject getSubject() {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<Subject>() {
                public Subject run() {
                    Subject subject = null;
                    SecurityContext sc = getSecurityContext();
                    if (sc != null) {
                        subject = sc.getUtil().getSubject();
                    }
                    return subject;
                }
            });
        } else {
            Subject subject = null;
            SecurityContext sc = getSecurityContext();
            if (sc != null) {
                subject = sc.getUtil().getSubject();
            }
            return subject;
        }
    }
}

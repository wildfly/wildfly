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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;

/**
 * Privileged Actions
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 12, 2011
 */
class SecurityActions {
    /**
     * Get the Thread Context Class Loader
     *
     * @return
     */
    static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Set the Thread Context {@code ClassLoader}
     *
     * @param cl An instance of {@code ClassLoader}
     */
    static void setContextClassLoader(final ClassLoader cl) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(cl);
                return null;
            }
        });
    }

    /**
     * Get the Class {@code ClassLoader}
     *
     * @param clazz A class whose classloader is needed
     * @return The Classloader of the clazz
     */
    static ClassLoader getClassLoader(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return clazz.getClassLoader();
            }
        });
    }

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
                    throw new IllegalStateException("SecurityContext is null");
                sc.setOutgoingRunAs(principal);
                return null;
            }
        });
    }

    /**
     * Removes the run as identity
     * @return the identity removed
     */
    static RunAs popRunAsIdentity() {
        return AccessController.doPrivileged(new PrivilegedAction<RunAs>() {

            @Override
            public RunAs run() {
                SecurityContext sc = getSecurityContext();
                if (sc == null)
                    throw new IllegalStateException("SecurityContext is null");
                RunAs principal = sc.getOutgoingRunAs();
                sc.setOutgoingRunAs(null);
                return principal;
            }
        });
    }

}
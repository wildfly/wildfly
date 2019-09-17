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

package org.wildfly.extension.undertow.security.jaspi;

import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;

import javax.security.auth.message.config.AuthConfigFactory;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Privileged Actions
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 12, 2011
 */
class SecurityActions {

    private static final PrivilegedAction<AuthConfigFactory> GET_AUTH_CONFIG_FACTORY_ACTION = new PrivilegedAction<AuthConfigFactory>() {

        @Override
        public AuthConfigFactory run() {
            return AuthConfigFactory.getFactory();
        }

    };

    static AuthConfigFactory getAuthConfigFactory() {
        return WildFlySecurityManager.isChecking() ?  doPrivileged(GET_AUTH_CONFIG_FACTORY_ACTION) : GET_AUTH_CONFIG_FACTORY_ACTION.run();
    }

    /**
     * Get the current {@code SecurityContext}
     *
     * @return an instance of {@code SecurityContext}
     */
    public static SecurityContext getSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<SecurityContext>() {
                @Override
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        } else {
            return SecurityContextAssociation.getSecurityContext();
        }
    }

}

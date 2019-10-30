/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.deployment;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class SecurityContextHandle implements ContextHandle {
    private final SecurityContext securityContext;

    SecurityContextHandle() {
        this.securityContext = getSecurityContext();
    }

    @Override
    public Handle setup() {
        final SecurityContext current = getSecurityContext();
        setSecurityContext(securityContext);
        return new Handle() {
            @Override
            public void tearDown() {
                setSecurityContext(current);
            }
        };
    }

    private static SecurityContext getSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
                @Override
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        }
        return SecurityContextAssociation.getSecurityContext();
    }

    private static void setSecurityContext(final SecurityContext securityContext) {
        if (WildFlySecurityManager.isChecking()) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    SecurityContextAssociation.setSecurityContext(securityContext);
                    return null;
                }
            });
        } else {
            SecurityContextAssociation.setSecurityContext(securityContext);
        }
    }
}

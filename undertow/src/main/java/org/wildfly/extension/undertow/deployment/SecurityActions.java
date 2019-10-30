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

package org.wildfly.extension.undertow.deployment;

import java.security.PrivilegedAction;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;

import javax.security.auth.Subject;

/**
 * Privileged blocks for this package
 */
class SecurityActions {

    static SecurityContext getSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<SecurityContext>() {
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        } else {
            return SecurityContextAssociation.getSecurityContext();
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
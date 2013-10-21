/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import javax.security.auth.Subject;

import org.jboss.as.core.security.AccessMechanism;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The context used to store state related to access control and auditing for the current invocation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AccessAuditContext {

    private static final RuntimePermission GET_ACCESS_AUDIT_CONTEXT = new RuntimePermission(
            "org.jboss.as.controller.GET_ACCESS_AUDIT_CONTEXT");

    private static ThreadLocal<AccessAuditContext> contextThreadLocal = new ThreadLocal<AccessAuditContext>();

    private String domainUuid;
    private AccessMechanism accessMechanism;

    private AccessAuditContext() {
        // This can only be instantiated as part of the doAs call.
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public AccessMechanism getAccessMechanism() {
        return accessMechanism;
    }

    public void setAccessMechanism(AccessMechanism accessMechanism) {
        this.accessMechanism = accessMechanism;
    }

    /**
     * Obtain the current {@link AccessAuditContext} or {@code null} if none currently set.
     *
     * @return The current {@link AccessAuditContext}
     * @deprecated Internal use, will be changed without warning at any time.
     */
    @Deprecated
    public static AccessAuditContext currentAccessAuditContext() {
        if (WildFlySecurityManager.isChecking()) {
            System.getSecurityManager().checkPermission(GET_ACCESS_AUDIT_CONTEXT);
        }
        return contextThreadLocal.get();
    }

    public static <T> T doAs(final Subject subject, final java.security.PrivilegedAction<T> action) {
        final AccessAuditContext previous = contextThreadLocal.get();
        try {
            contextThreadLocal.set(new AccessAuditContext());
            return Subject.doAs(subject, action);
        } finally {
            contextThreadLocal.set(previous);
        }
    }

    public static <T> T doAs(Subject subject, java.security.PrivilegedExceptionAction<T> action)
            throws java.security.PrivilegedActionException {
        final AccessAuditContext previous = contextThreadLocal.get();
        try {
            contextThreadLocal.set(new AccessAuditContext());
            return Subject.doAs(subject, action);
        } finally {
            contextThreadLocal.set(previous);
        }
    }

}

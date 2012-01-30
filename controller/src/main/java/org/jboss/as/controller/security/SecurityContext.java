/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.security;

import java.security.Permission;

import javax.security.auth.Subject;

/**
 * A simple SecurityContext to manage associating the Subject of the current request with the current thread.
 *
 * PLEASE NOTE - This is an internal API so is subject to change in future releases.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityContext {

    private static final RuntimePermission GET_SUBJECT_PERMISSION = new RuntimePermission("org.jboss.as.controller.security.GET_SUBJECT");
    private static final RuntimePermission SET_SUBJECT_PERMISSION = new RuntimePermission("org.jboss.as.controller.security.SET_SUBJECT");
    private static final RuntimePermission CLEAR_SUBJECT_PERMISSION = new RuntimePermission("org.jboss.as.controller.security.CLEAR_SUBJECT");

    private static final ThreadLocal<Subject> subject = new ThreadLocal<Subject>();

    /**
     *
     *
     * @return The Subject associated with this SecurityContext.
     */
    public static Subject getSubject() {
        checkPermission(GET_SUBJECT_PERMISSION);

        return subject.get();
    }

    public static void setSubject(final Subject subject) {
        checkPermission(SET_SUBJECT_PERMISSION);

        SecurityContext.subject.set(subject);
    }

    public static void clearSubject() {
        checkPermission(CLEAR_SUBJECT_PERMISSION);

        subject.set(null);
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }


    }

}

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

package org.jboss.as.controller.remote;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.jboss.as.controller.security.SecurityContext;

/**
 * Security Actions for classes in the org.jboss.as.controller.remote package.
 *
 * No methods in this class are to be made public under any circumstances!
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

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

    static void setSecurityContextSubject(final Subject subject) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                SecurityContext.setSubject(subject);
                return null;
            }
        });
    }

    static void clearSubjectSecurityContext() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                SecurityContext.clearSubject();
                return null;
            }
        });
    }

}

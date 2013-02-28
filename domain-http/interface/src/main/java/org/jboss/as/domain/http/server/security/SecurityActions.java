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

package org.jboss.as.domain.http.server.security;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.jboss.as.controller.security.SecurityContext;
import org.jboss.as.util.security.ReadPropertyAction;

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

/**
 * Security Actions for classes in the org.jboss.as.domain.http.server.security package.
 *
 * No methods in this class are to be made public under any circumstances!
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

    static String getStringProperty(final String key) {
        return getSecurityManager() == null ? getProperty(key) : doPrivileged(new ReadPropertyAction(key));
    }

    static boolean getBoolean(final String key) {
        return Boolean.parseBoolean(getStringProperty(key));
    }

    static int getInt(final String key, final int def) {
        try {
            final String value = getStringProperty(key);
            return value == null ? def : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    static void setSecurityContextSubject(final Subject subject) {
        doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                SecurityContext.setSubject(subject);
                return null;
            }
        });
    }

    static void clearSubjectSecurityContext() {
        doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                SecurityContext.clearSubject();
                return null;
            }
        });
    }

}

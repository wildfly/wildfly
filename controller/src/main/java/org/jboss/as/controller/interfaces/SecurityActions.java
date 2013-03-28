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

package org.jboss.as.controller.interfaces;

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import org.wildfly.security.manager.GetBooleanAction;

/**
 * Security actions to perform possibly privileged operations.  No methods in
 * this class are to be made public under any circumstances!
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class SecurityActions {

    static boolean getBoolean(String systemProperty) {
        return getSecurityManager() == null ? Boolean.getBoolean(systemProperty) : doPrivileged(new GetBooleanAction(systemProperty));
    }
}

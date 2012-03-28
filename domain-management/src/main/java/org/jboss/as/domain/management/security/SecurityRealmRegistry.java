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

package org.jboss.as.domain.management.security;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.domain.management.SecurityRealm;

/**
 * A registry to store references to currently available realms.
 *
 * PLEASE NOTE - This is an internal API so is subject to change in future releases.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmRegistry {

    private static final RuntimePermission LOOKUP_REALM_PERMISSION = new RuntimePermission("org.jboss.as.domain.management.security.LOOKUP_REALM");

    private static Map<String, SecurityRealm> realms = new HashMap<String, SecurityRealm>();

    public static SecurityRealm lookup(final String name) {
        checkPermission(LOOKUP_REALM_PERMISSION);

        return realms.get(name);
    }

    static void register(final String name, final SecurityRealm realm) {
        // No permission check as we are only called from this package, if visibility
        // is increased a permission should be added.

        realms.put(name, realm);
    }

    static void remove(final String name) {
        // No permission check as we are only called from this package, if visibility
        // is increased a permission should be added.

        realms.remove(name);
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }

}

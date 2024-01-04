/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.module;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Utility class which will be placed in a custom module deployed to the AS. It has only limited set of permissions granted in
 * its module.xml descriptor.
 *
 * @author Josef Cacek
 */
public class CheckJSMUtils {

    public static String getSystemProperty(final String propName) throws IllegalStateException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(propName);
                }
            });
        }
        throw new IllegalStateException("Java Security Manager is not initialized");
    }

    public static void checkRuntimePermission(final String permissionName) throws IllegalStateException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    sm.checkPermission(new RuntimePermission(permissionName));
                    return null;
                }
            });
        } else {
            throw new IllegalStateException("Java Security Manager is not initialized");
        }
    }

}

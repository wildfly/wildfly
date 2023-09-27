/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman;

/**
 * Helper class for permissions testing. This class is usually packaged to a library-like archive in a test deployment.
 *
 * @author Josef Cacek
 */
public class PermissionUtil {

    /**
     * Simply calls {@link System#getProperty(String)} method.
     *
     * @param property
     * @return system property
     */
    public static String getSystemProperty(String property) {
        return System.getProperty(property);
    }
}

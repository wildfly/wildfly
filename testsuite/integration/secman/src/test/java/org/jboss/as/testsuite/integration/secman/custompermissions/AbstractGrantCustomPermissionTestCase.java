/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.custompermissions;

import org.junit.Test;

public abstract class AbstractGrantCustomPermissionTestCase {

    private final String moduleName;

    protected AbstractGrantCustomPermissionTestCase(final String moduleName) {
        this.moduleName = moduleName;
    }

    protected void checkCustomPermission(String customPermName) {
        if (customPermName != null) {
            try {
                final SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkPermission(new CustomPermission(customPermName));
                } else {
                    throw new IllegalStateException("Java Security Manager is not initialized");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(e);
            }
        }
    }

    /**
     * Test which checks a custom permission.
     */
    @Test
    public void testMinimumPermission() throws Exception {

        checkCustomPermission(moduleName);
    }

    /**
     * Test which checks we don't have custom permission.
     */
    @Test(expected = AssertionError.class)
    public void testNoCustomPermissionPermissionsXML() throws Exception {
        String customPermName = "org.jboss.test-wrong";

        checkCustomPermission(customPermName);
    }
}

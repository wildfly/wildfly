/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks if empty permissions set is used for an installed module when empty <code>&lt;permissions&gt;</code>
 * element is provided in module.xml.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(DenyModulePermissionsTestCase.CustomModuleSetup.class)
public class DenyModulePermissionsTestCase {

    /**
     * Creates test archive.
     *
     * @return test application
     */
    @Deployment()
    public static JavaArchive deployment() {
        return ShrinkWrap
                .create(JavaArchive.class, "modperm-deny.jar")
                .addClass(AbstractCustomModuleServerSetup.class)
                .addAsManifestResource(
                        Utils.getJBossDeploymentStructure(AbstractCustomModuleServerSetup.MODULE_NAME_BASE + ".deny"),
                        "jboss-deployment-structure.xml");
    }

    /**
     * Test which reads a system property.
     */
    @Test
    public void testReadJavaHome() {
        try {
            CheckJSMUtils.getSystemProperty("java.home");
            fail("Access should be denied");
        } catch (AccessControlException e) {
            Permission expectedPerm = new PropertyPermission("java.home", "read");
            assertEquals("Permission type doesn't match", expectedPerm, e.getPermission());
        }
    }

    /**
     * Test which checks a custom permission which should not be granted.
     */
    @Test
    public void testCustomPermission() {
        final String permissionName = "org.jboss.security.Permission";
        try {
            CheckJSMUtils.checkRuntimePermission(permissionName);
            fail("Access should be denied");
        } catch (AccessControlException e) {
            Permission expectedPerm = new RuntimePermission(permissionName);
            assertEquals("Permission type doesn't match", expectedPerm, e.getPermission());
        }
    }

    static class CustomModuleSetup extends AbstractCustomModuleServerSetup {
        @Override
        protected String getModuleSuffix() {
            return "deny";
        }
    }
}

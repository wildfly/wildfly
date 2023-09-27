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
 * Test case, which checks if limited permissions set defined in module.xml is used for an installed module.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(LimitedModulePermissionsTestCase.CustomModuleSetup.class)
public class LimitedModulePermissionsTestCase {

    /**
     * Creates test archive.
     *
     * @return test application
     */
    @Deployment()
    public static JavaArchive deployment() {
        return ShrinkWrap
                .create(JavaArchive.class, "modperm-limited.jar")
                .addClass(AbstractCustomModuleServerSetup.class)
                .addAsManifestResource(
                        Utils.getJBossDeploymentStructure(AbstractCustomModuleServerSetup.MODULE_NAME_BASE + ".limited"),
                        "jboss-deployment-structure.xml");
    }

    /**
     * Test which reads system property without Permission.
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
     * Test which checks custom permission which should not be granted.
     */
    @Test
    public void testCustomPermWithoutGrant() {
        final String permissionName = "org.jboss.security.Permission";
        try {
            CheckJSMUtils.checkRuntimePermission(permissionName);
            fail("Access should be denied");
        } catch (AccessControlException e) {
            Permission expectedPerm = new RuntimePermission(permissionName);
            assertEquals("Permission type doesn't match", expectedPerm, e.getPermission());
        }
    }

    /**
     * Test which checks granted permission.
     */
    @Test
    public void testGrantedCustomPerm() {
        CheckJSMUtils.checkRuntimePermission("secman.test.Permission");
    }

    static class CustomModuleSetup extends AbstractCustomModuleServerSetup {
        @Override
        protected String getModuleSuffix() {
            return "limited";
        }
    }

}

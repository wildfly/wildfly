/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.module;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks if full permissions set is used for an installed module when no <code>&lt;permissions&gt;</code>
 * element is provided in module.xml.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(GrantModulePermissionsTestCase.CustomModuleSetup.class)
public class GrantModulePermissionsTestCase {

    /**
     * Creates test archive.
     *
     * @return test application
     */
    @Deployment()
    public static JavaArchive deployment() {
        return ShrinkWrap
                .create(JavaArchive.class, "modperm-grant.jar")
                .addClass(AbstractCustomModuleServerSetup.class)
                .addAsManifestResource(
                        Utils.getJBossDeploymentStructure(AbstractCustomModuleServerSetup.MODULE_NAME_BASE + ".grant"),
                        "jboss-deployment-structure.xml");
    }

    /**
     * Test which reads system property.
     */
    @Test
    public void testReadJavaHome() {
        CheckJSMUtils.getSystemProperty("java.home");
    }

    /**
     * Test which checks a custom permission.
     */
    @Test
    public void testCustomPermission() {
        CheckJSMUtils.checkRuntimePermission("org.jboss.security.Permission");
    }

    static class CustomModuleSetup extends AbstractCustomModuleServerSetup {
        @Override
        protected String getModuleSuffix() {
            return "grant";
        }
    }

}

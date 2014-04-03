/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

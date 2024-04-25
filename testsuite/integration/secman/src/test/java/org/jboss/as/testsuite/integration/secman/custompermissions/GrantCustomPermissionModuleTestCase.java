/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.custompermissions;

import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.testing.tools.deployments.DeploymentDescriptors;

/**
 * Test case which checks if permissions.xml which contains CustomPermission
 * permission works right. https://issues.jboss.org/browse/JBEAP-903
 *
 * @author Hynek Švábek <hsvabek@redhat.com>
 *
 */
@RunWith(Arquillian.class)
@ServerSetup(GrantCustomPermissionModuleTestCase.CustomPermissionModuleServerSetupTask.class)
public class GrantCustomPermissionModuleTestCase extends AbstractGrantCustomPermissionTestCase {
    private static final String MODULE_NAME = "org.jboss.test.deployment.permission";

    private static final String DEP_PERMISSIONS_XML_NAME = "DEP_PERMISSIONS_XML.war";

    public GrantCustomPermissionModuleTestCase() {
        super(MODULE_NAME);
    }

    /**
     * Creates test web archive.
     *
     * @return test application
     */
    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap
                .create(WebArchive.class, DEP_PERMISSIONS_XML_NAME)
                .addClasses(AbstractGrantCustomPermissionTestCase.class, AbstractCustomPermissionServerSetup.class)
                .addAsManifestResource(DeploymentDescriptors.createPermissionsXmlAsset(new CustomPermission(MODULE_NAME)), "permissions.xml")
                .addAsManifestResource(DeploymentDescriptors.createJBossDeploymentStructureAsset(Set.of(MODULE_NAME), Set.of()), "jboss-deployment-structure.xml");
    }

    static class CustomPermissionModuleServerSetupTask extends AbstractCustomPermissionServerSetup {
        CustomPermissionModuleServerSetupTask() {
            super(MODULE_NAME);
        }
    }
}

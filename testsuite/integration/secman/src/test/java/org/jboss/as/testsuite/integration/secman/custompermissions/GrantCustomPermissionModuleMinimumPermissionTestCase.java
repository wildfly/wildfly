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
 * Test case which checks if subsystem with minimumPermissions setting which
 * contains CustomPermission permission works right.
 * https://issues.jboss.org/browse/JBEAP-903
 *
 * @author Hynek Švábek <hsvabek@redhat.com>
 *
 */
@RunWith(Arquillian.class)
@ServerSetup(GrantCustomPermissionModuleMinimumPermissionTestCase.GrantCustomPermissionModuleMinimumPermissionServerSetupTask.class)
public class GrantCustomPermissionModuleMinimumPermissionTestCase extends AbstractGrantCustomPermissionTestCase {
    private static final String MODULE_NAME = "org.jboss.test.module.permission";

    private static final String DEP_WITHOUT_PERMISSIONS_XML_NAME = "DEP_WITHOUT_PERMISSIONS_XML.war";

    public GrantCustomPermissionModuleMinimumPermissionTestCase() {
        super(MODULE_NAME);
    }

    /**
     * Creates test web archive.
     *
     * @return test application
     */
    @Deployment
    public static WebArchive deployment() {
        return DeploymentDescriptors.addJBossDeploymentStructure(ShrinkWrap.create(WebArchive.class, DEP_WITHOUT_PERMISSIONS_XML_NAME)
                .addClasses(AbstractGrantCustomPermissionTestCase.class, AbstractCustomPermissionServerSetup.class), Set.of(MODULE_NAME), Set.of());
    }

    static class GrantCustomPermissionModuleMinimumPermissionServerSetupTask extends AbstractCustomPermissionServerSetup {

        GrantCustomPermissionModuleMinimumPermissionServerSetupTask() {
            super(MODULE_NAME);
        }

        @Override
        protected boolean writeMinimumPermissions() {
            return true;
        }
    }
}

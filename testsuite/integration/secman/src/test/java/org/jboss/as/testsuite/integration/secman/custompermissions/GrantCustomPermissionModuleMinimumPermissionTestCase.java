/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.custompermissions;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

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

    private static final String DEP_WITHOUT_PERMISSIONS_XML_NAME = "DEP_WITHOUT_PERMISSIONS_XML.war";

    /**
     * Creates test web archive.
     *
     * @return test application
     */
    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, DEP_WITHOUT_PERMISSIONS_XML_NAME)
                .addClasses(AbstractGrantCustomPermissionTestCase.class, AbstractCustomPermissionServerSetup.class)
                .addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.test"), "jboss-deployment-structure.xml");
    }

    static class GrantCustomPermissionModuleMinimumPermissionServerSetupTask extends AbstractCustomPermissionServerSetup {

        @Override
        protected boolean writeMinimumPermissions() {
            return true;
        }
    }
}

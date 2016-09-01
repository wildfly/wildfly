/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

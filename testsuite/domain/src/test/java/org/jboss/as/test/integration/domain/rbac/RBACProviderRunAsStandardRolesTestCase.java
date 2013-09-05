/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.rbac;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.suites.FullRbacProviderRunAsTestSuite;
import org.jboss.as.test.integration.domain.suites.FullRbacProviderTestSuite;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Tests of the standard roles using the "rbac" access control provider but with
 * the client using operation-headers to control the roles.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class RBACProviderRunAsStandardRolesTestCase extends AbstractStandardRolesTestCase {

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = FullRbacProviderRunAsTestSuite.createSupport(RBACProviderRunAsStandardRolesTestCase.class.getSimpleName());
        masterClientConfig = testSupport.getDomainMasterConfiguration();
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.setup(domainClient);
        deployDeployment1(domainClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {

        try {
            UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.tearDown(testSupport.getDomainMasterLifecycleUtil().getDomainClient());
        } finally {
            try {
                removeDeployment1(testSupport.getDomainMasterLifecycleUtil().getDomainClient());
            } finally {
                FullRbacProviderTestSuite.stopSupport();
                testSupport = null;
            }
        }
    }

    @Override
    protected boolean isAllowLocalAuth() {
        return true;
    }

    @Override
    protected void configureRoles(ModelNode op, String[] roles) {
        RbacUtil.addRoleHeader(op, roles);
    }
}

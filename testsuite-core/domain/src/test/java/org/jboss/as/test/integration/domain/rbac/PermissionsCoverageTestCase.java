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

import static org.jboss.as.test.integration.management.rbac.PermissionsCoverageTestUtil.assertTheEntireDomainTreeHasPermissionsDefined;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.suites.FullRbacProviderTestSuite;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class PermissionsCoverageTestCase extends AbstractRbacTestCase {
    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = FullRbacProviderTestSuite.createSupport(PermissionsCoverageTestCase.class.getSimpleName());
        masterClientConfig = testSupport.getDomainMasterConfiguration();
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.setup(domainClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        try {
            UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.tearDown(testSupport.getDomainMasterLifecycleUtil().getDomainClient());
        } finally {
            FullRbacProviderTestSuite.stopSupport();
            testSupport = null;
        }
    }

    @Override
    protected void configureRoles(ModelNode op, String[] roles) {
        // no-op. Role mapping is done based on the client's authenticated Subject
    }

    @Test
    public void testTheEntireDomainTreeHasPermissionsDefined() throws IOException {
        ModelControllerClient client = getClientForUser(RbacUtil.SUPERUSER_USER, true, masterClientConfig);
        assertTheEntireDomainTreeHasPermissionsDefined(client);
    }
}

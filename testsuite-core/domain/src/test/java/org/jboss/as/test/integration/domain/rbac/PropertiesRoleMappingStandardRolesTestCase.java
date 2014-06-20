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

import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.suites.FullRbacProviderPropertiesRoleMappingTestSuite;
import org.jboss.as.test.integration.management.rbac.GroupRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Tests of the standard roles authorized against .properties file using the "rbac" access control provider.
 *
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class PropertiesRoleMappingStandardRolesTestCase extends AbstractStandardRolesTestCase {
    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = FullRbacProviderPropertiesRoleMappingTestSuite.createSupport(PropertiesRoleMappingStandardRolesTestCase.class.getSimpleName());
        masterClientConfig = testSupport.getDomainMasterConfiguration();
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        StandardRolesMappingSetup.INSTANCE.setup(domainClient);
        deployDeployment1(domainClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        try {
            StandardRolesMappingSetup.INSTANCE.tearDown(domainClient);
        } finally {
            try {
                removeDeployment1(domainClient);
            } finally {
                FullRbacProviderPropertiesRoleMappingTestSuite.stopSupport();
                testSupport = null;
            }
        }
    }

    @Override
    protected boolean isAllowLocalAuth() {
        return false;
    }

    @Override
    protected void configureRoles(ModelNode op, String[] roles) {
        // no-op. Role mapping is done based on the client's authenticated Subject
    }

    static class StandardRolesMappingSetup extends GroupRolesMappingServerSetupTask {
        private static final Map<String, Set<String>> STANDARD_ROLES_TO_GROUPS;

        static {
            Map<String, Set<String>> rolesToGroups = new HashMap<String, Set<String>>();
            rolesToGroups.put(MONITOR_ROLE, Collections.singleton(MONITOR_USER));
            rolesToGroups.put(OPERATOR_ROLE, Collections.singleton(OPERATOR_USER));
            rolesToGroups.put(MAINTAINER_ROLE, Collections.singleton(MAINTAINER_USER));
            rolesToGroups.put(DEPLOYER_ROLE, Collections.singleton(DEPLOYER_USER));
            rolesToGroups.put(ADMINISTRATOR_ROLE, Collections.singleton(ADMINISTRATOR_USER));
            rolesToGroups.put(AUDITOR_ROLE, Collections.singleton(AUDITOR_USER));
            rolesToGroups.put(SUPERUSER_ROLE, Collections.singleton(SUPERUSER_USER));
            STANDARD_ROLES_TO_GROUPS = rolesToGroups;
        }

        static final StandardRolesMappingSetup INSTANCE = new StandardRolesMappingSetup();

        protected StandardRolesMappingSetup() {
            super(STANDARD_ROLES_TO_GROUPS);
        }
    }
}

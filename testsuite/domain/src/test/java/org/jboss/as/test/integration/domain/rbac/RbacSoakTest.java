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

import static org.junit.Assert.fail;

import java.util.Random;

import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.suites.FullRbacProviderTestSuite;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class RbacSoakTest extends AbstractRbacTestCase {
    private static final Logger log = Logger.getLogger(RbacSoakTest.class);

    private static final int numClients = Integer.parseInt(System.getProperty("jboss.test.rbac.soak.clients"));
    private static final int numIterations = Integer.parseInt(System.getProperty("jboss.test.rbac.soak.iterations"));

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = FullRbacProviderTestSuite.createSupport(RbacSoakTest.class.getSimpleName());
        masterClientConfig = testSupport.getDomainMasterConfiguration();
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.setup(domainClient);
        AbstractServerGroupScopedRolesTestCase.setupRoles(domainClient);
        RBACProviderServerGroupScopedRolesTestCase.ServerGroupRolesMappingSetup.INSTANCE.setup(domainClient);
        AbstractHostScopedRolesTestCase.setupRoles(domainClient);
        RBACProviderHostScopedRolesTestCase.HostRolesMappingSetup.INSTANCE.setup(domainClient);
        deployDeployment1(domainClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        try {
            RBACProviderHostScopedRolesTestCase.HostRolesMappingSetup.INSTANCE.tearDown(domainClient);
        } finally {
            try {
                AbstractHostScopedRolesTestCase.tearDownRoles(domainClient);
            } finally {
                try {
                    RBACProviderServerGroupScopedRolesTestCase.ServerGroupRolesMappingSetup.INSTANCE.tearDown(domainClient);
                } finally {
                    try {
                        AbstractServerGroupScopedRolesTestCase.tearDownRoles(domainClient);
                    } finally {
                        try {
                            UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.tearDown(domainClient);
                        } finally {
                            try {
                                removeDeployment1(domainClient);
                            } finally {
                                FullRbacProviderTestSuite.stopSupport();
                                testSupport = null;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void configureRoles(ModelNode op, String[] roles) {
        // no-op. Role mapping is done based on the client's authenticated Subject
    }

    @Test
    public void soakTest() {
        log.info("Starting RBAC soak test: " + numClients + " concurrent clients, " + numIterations + " iterations each");

        TestClient[] clients = new TestClient[numClients];
        for (int i = 0; i < numClients; i++) {
            TestClient client = new TestClient(i + 1, pickTestClientType(), pickRole());
            client.start();
            clients[i] = client;
        }

        int failures = 0;
        for (TestClient client : clients) {
            try {
                client.join();
            } catch (InterruptedException e) {
            }
            if (client.failed) {
                failures++;
            }
        }

        log.info("Finished RBAC soak test with " + failures + " clients failing");

        if (failures > 0) {
            fail("Got " + failures + " clients failing");
        }
    }

    // test utils

    private final Random random = new Random();

    private TestClientType pickTestClientType() {
        TestClientType[] allTypes = TestClientType.values();
        int i = random.nextInt(allTypes.length);
        return allTypes[i];
    }

    private StandardRole pickRole() {
        StandardRole[] allRoles = StandardRole.values();
        int i = random.nextInt(allRoles.length);
        return allRoles[i];
    }

    private static enum TestClientType { STANDARD, SERVER_GROUP_SCOPED, HOST_SCOPED }

    private static final class TestClient extends Thread {
        private final String description;
        private final TestClientType type;
        private final StandardRole role;
        private boolean failed = false;

        private TestClient(int id, TestClientType type, StandardRole role) {
            super("TestClient-" + id + " (" + type.toString().toLowerCase() + " " + role.toString().toLowerCase() + ")");
            this.description = "TestClient-" + id + " (" + type.toString().toLowerCase() + " " + role.toString().toLowerCase() + ")";
            this.type = type;
            this.role = role;
        }

        @Override
        public void run() {
            log.info("Started " + this.toString());
            try {
                for (int i = 0; i < numIterations; i++) {
                    log.debug("Running iteration " + (i + 1));
                    runTest();
                }
                log.info("Finished successfully " + description);
            } catch (Throwable e) { // need to catch AssertionError
                failed = true;
                log.error("Failed " + description, e);
            }
        }

        private void runTest() throws Exception {
            RbacDomainRolesTests test;

            switch (type) {
                case STANDARD: test = new RBACProviderStandardRolesTestCase(); break;
                case SERVER_GROUP_SCOPED: test = new RBACProviderServerGroupScopedRolesTestCase(); break;
                case HOST_SCOPED: test = new RBACProviderHostScopedRolesTestCase(); break;
                default: throw new AssertionError();
            }

            ((AbstractRbacTestCase) test).readOnly = true;

            try {
                switch (role) {
                    case MONITOR: test.testMonitor(); break;
                    case OPERATOR: test.testOperator(); break;
                    case MAINTAINER: test.testMaintainer(); break;
                    case DEPLOYER: test.testDeployer(); break;
                    case ADMINISTRATOR: test.testAdministrator(); break;
                    case AUDITOR: test.testAuditor(); break;
                    case SUPERUSER: test.testSuperUser(); break;
                    default: throw new AssertionError();
                }
            } finally {
                test.tearDown();
            }
        }
    }
}

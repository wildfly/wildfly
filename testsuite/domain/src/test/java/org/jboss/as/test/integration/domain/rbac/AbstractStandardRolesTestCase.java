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

import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.junit.After;
import org.junit.Test;

/**
 * Abstract superclass of access control provider test cases covering the standard roles.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class AbstractStandardRolesTestCase extends AbstractRbacTestCase {


    @After
    public void tearDown() throws IOException {
        AssertionError assertionError = null;
        try {
            removeResource(DEPLOYMENT_2);
        } catch (AssertionError e) {
            assertionError = e;
        } finally {
            removeResource(TEST_PATH);
        }


        if (assertionError != null) {
            throw assertionError;
        }
    }

    protected abstract boolean isAllowLocalAuth();

    @Test
    public void testMonitor() throws Exception {
        ModelControllerClient client = getClientForUser(MONITOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, MONITOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, MONITOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, MONITOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, MONITOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, MONITOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, MONITOR_USER);
        checkSensitiveAttribute(client, null, null, false, MONITOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, MONITOR_USER);
        runGC(client, MASTER, null, Outcome.UNAUTHORIZED, MONITOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.UNAUTHORIZED, MONITOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, MONITOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, MONITOR_USER);
    }

    @Test
    public void testOperator() throws Exception {
        ModelControllerClient client = getClientForUser(OPERATOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, OPERATOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, OPERATOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, OPERATOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, OPERATOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, OPERATOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, OPERATOR_USER);
        checkSensitiveAttribute(client, null, null, false, OPERATOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, OPERATOR_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, OPERATOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, OPERATOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, OPERATOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, OPERATOR_USER);
    }

    @Test
    public void testMaintainer() throws Exception {
        ModelControllerClient client = getClientForUser(MAINTAINER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, MAINTAINER_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, MAINTAINER_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, MAINTAINER_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, MAINTAINER_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, MAINTAINER_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, MAINTAINER_USER);
        checkSensitiveAttribute(client, null, null, false, MAINTAINER_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, MAINTAINER_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, MAINTAINER_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, MAINTAINER_USER);
        addDeployment2(client, Outcome.SUCCESS, MAINTAINER_USER);
        addPath(client, Outcome.SUCCESS, MAINTAINER_USER);
    }

    @Test
    public void testDeployer() throws Exception {
        ModelControllerClient client = getClientForUser(DEPLOYER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, DEPLOYER_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, DEPLOYER_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, DEPLOYER_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, DEPLOYER_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, DEPLOYER_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, DEPLOYER_USER);
        checkSensitiveAttribute(client, null, null, false, DEPLOYER_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, DEPLOYER_USER);
        runGC(client, MASTER, null, Outcome.UNAUTHORIZED, DEPLOYER_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.UNAUTHORIZED, DEPLOYER_USER);
        addDeployment2(client, Outcome.SUCCESS, DEPLOYER_USER);
        addPath(client, Outcome.UNAUTHORIZED, DEPLOYER_USER);
    }

    @Test
    public void testAdministrator() throws Exception {
        ModelControllerClient client = getClientForUser(ADMINISTRATOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, ADMINISTRATOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSensitiveAttribute(client, null, null, true, ADMINISTRATOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, true, ADMINISTRATOR_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, ADMINISTRATOR_USER);
        addDeployment2(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
        addPath(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
    }

    @Test
    public void testAuditor() throws Exception {
        ModelControllerClient client = getClientForUser(AUDITOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, AUDITOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, AUDITOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, AUDITOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, AUDITOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.SUCCESS, AUDITOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, AUDITOR_USER);
        checkSensitiveAttribute(client, null, null, true, AUDITOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, true, AUDITOR_USER);
        runGC(client, MASTER, null, Outcome.UNAUTHORIZED, AUDITOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.UNAUTHORIZED, AUDITOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
    }

    @Test
    public void testSuperUser() throws Exception {
        ModelControllerClient client = getClientForUser(SUPERUSER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, SUPERUSER_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, SUPERUSER_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, SUPERUSER_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, SUPERUSER_USER);
        checkSecurityDomainRead(client, null, null, Outcome.SUCCESS, SUPERUSER_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, SUPERUSER_USER);
        checkSensitiveAttribute(client, null, null, true, SUPERUSER_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, true, SUPERUSER_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, SUPERUSER_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, SUPERUSER_USER);
        addDeployment2(client, Outcome.SUCCESS, SUPERUSER_USER);
        addPath(client, Outcome.SUCCESS, SUPERUSER_USER);
    }

}

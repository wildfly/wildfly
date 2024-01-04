/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.mgmt.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReadFullModelTestCase extends AbstractRbacTestCase {

    @Test
    public void testMonitor() throws Exception {
        ModelControllerClient client = getClientForUser(MONITOR_USER);
        whoami(client, MONITOR_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    @Test
    public void testOperator() throws Exception {
        ModelControllerClient client = getClientForUser(OPERATOR_USER);
        whoami(client, OPERATOR_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    @Test
    public void testMaintainer() throws Exception {
        ModelControllerClient client = getClientForUser(MAINTAINER_USER);
        whoami(client, MAINTAINER_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    @Test
    public void testDeployer() throws Exception {
        ModelControllerClient client = getClientForUser(DEPLOYER_USER);
        whoami(client, DEPLOYER_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    @Test
    public void testAdministrator() throws Exception {
        ModelControllerClient client = getClientForUser(ADMINISTRATOR_USER);
        whoami(client, ADMINISTRATOR_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    @Test
    public void testAuditor() throws Exception {
        ModelControllerClient client = getClientForUser(AUDITOR_USER);
        whoami(client, AUDITOR_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    @Test
    public void testSuperUser() throws Exception {
        ModelControllerClient client = getClientForUser(SUPERUSER_USER);
        whoami(client, SUPERUSER_USER);
        readWholeConfig(client, Outcome.SUCCESS);
    }

    private static void whoami(ModelControllerClient client, String expectedUsername) throws IOException {
        ModelNode op = createOpNode(null, "whoami");
        op.get("verbose").set(true);
        ModelNode result = RbacUtil.executeOperation(client, op, Outcome.SUCCESS);
        System.out.println("whomai " + result);
        String returnedUsername = result.get(RESULT, "identity", USERNAME).asString();
        assertEquals(expectedUsername, returnedUsername);
    }

    private void readWholeConfig(ModelControllerClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode("/", READ_RESOURCE_OPERATION);
        op.get("include-runtime").set(true);
        op.get("recursive").set(true);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

}

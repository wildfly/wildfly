/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.integration.elytron.realm;


import static org.junit.Assert.assertEquals;

import org.jboss.dmr.ModelNode;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.ServerReload;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.wildfly.test.security.common.elytron.JdbcSecurityRealm;


/**
 * A test case to test adding a {@link JdbcSecurityRealm} within the Elytron subsystem in admin only mode.
 *
 * @author <a href="mailto:aabdelsa@redhat.com">Ashley Abdel-Sayed</a>
 */
@RunWith(Arquillian.class)
public class JDBCRealmAdminOnlyModeTestCase {

    @ContainerResource
    protected ManagementClient managementClient;

    protected static final int CONNECTION_TIMEOUT_IN_MS = TimeoutUtil.adjust(6 * 1000);
    protected static final boolean ADMIN_ONLY_MODE = true;

    @Test
    @RunAsClient
    public void testAddJDBCRealmAdminOnlyMode() throws Exception {

        ModelControllerClient client = managementClient.getControllerClient();

        ServerReload.executeReloadAndWaitForCompletion(client, CONNECTION_TIMEOUT_IN_MS, ADMIN_ONLY_MODE,
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), null);

        ModelNode operation = Operations.createReadAttributeOperation(new ModelNode().setEmptyList(), "running-mode");
        assertEquals("ADMIN_ONLY", client.execute(operation).get("result").asString());

        ModelNode params = new ModelNode();
        params.get("sql").set("SELECT * FROM Users WHERE username = ?");
        params.get("data-source").set("ExampleDS");

        operation = Operations.createAddOperation(Operations.createAddress("subsystem", "elytron", "jdbc-realm", "MyRealm"));
        operation.get("principal-query").add(params);
        assertSuccess(client.execute(operation));

        operation = Operations.createRemoveOperation(Operations.createAddress("subsystem", "elytron", "jdbc-realm", "MyRealm"));
        assertSuccess(client.execute(operation));

    }

    @After
    public void reload() throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();
        ServerReload.executeReloadAndWaitForCompletion(client, CONNECTION_TIMEOUT_IN_MS, !ADMIN_ONLY_MODE,
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), null);
        ModelNode operation = Operations.createReadAttributeOperation(new ModelNode().setEmptyList(), "running-mode");
        assertEquals("NORMAL", client.execute(operation).get("result").asString());
    }

    private ModelNode assertSuccess(ModelNode response) {
        if (!Operations.isSuccessfulOutcome(response)) {
            Assert.fail(Operations.getFailureDescription(response).asString());
        }
        return response;
    }
}
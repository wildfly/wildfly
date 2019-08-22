/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.elytron.realm;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.AggregateSecurityRealm;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.FileSystemRealm;
import org.wildfly.test.security.common.elytron.RegexPrincipalTransformer;

import java.util.ArrayList;

/**
 * Negative scenarios for management operations about Elytron Aggregate Realm with Principal transformer
 *
 * https://issues.jboss.org/browse/WFLY-12202
 * https://issues.jboss.org/browse/WFCORE-4496
 * https://issues.jboss.org/browse/ELY-1829
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AggregateRealmWrongPrincipalSetupTestCase {

    private static final String AGGREGATE_REALM_NAME = "elytron-aggregate-realm-same-type";

    private static final String FILESYSTEM_REALM_AUTHN_NAME = "elytron-authn-filesystem-realm";
    private static final String FILESYSTEM_REALM_AUTHZ_NAME = "elytron-authz-filesystem-realm";

    private static final String VALID_PRINCIPAL_TRANSFORMER = "elytron-custom-principal-transformer";
    private static final String NON_EXISTING_PRINCIPAL_TRANSFORMER = "invalid-elytron-custom-principal-transformer";
    private static final String EMPTY_PRINCIPAL_TRANSFORMER = "";

    /**
     * Prerequisites for test case
     */
    private static InitSetupTask initSetupTask = new InitSetupTask();

    /**
     * Management client
     */
    private static final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    private static final ManagementClient mClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

    /**
     * Prepare prerequisites
     */
    @BeforeClass
    public static void prepare() throws Exception {
        initSetupTask.setup(mClient, null);
    }

    /**
     * Test cleanup
     */
    @AfterClass
    public static void cleanup() throws Exception {
        initSetupTask.tearDown(mClient, null);
    }

    /**
     * Adding an "aggregate-realm" with a "principal-transformer".
     * Non-existing principal transformer is used.
     * Check that correct error message is printed.
     */
    @Test
    public void nonExistingPrincipalTransformer() throws Exception {
        InvalidTransformerSetupTask task = new InvalidTransformerSetupTask();
        boolean expectedErrorFound = false;
        try {
            task.setup(mClient, null);
        } catch (Exception e)  {
            String msg = e.getMessage();
            if (msg.contains("WFLYCTL0369") && msg.contains(NON_EXISTING_PRINCIPAL_TRANSFORMER)) {
                expectedErrorFound = true;
            }
        }
        if (!expectedErrorFound) {
            try {
                task.tearDown(mClient, null);
            } catch (Exception e) {
                // tearDown needs to be done as a cleanup for sure
                // originally, realm should not be created, but expectation was not fulfilled
            }
            Assert.fail("Expected error containing WFLYCTL0369 and " + NON_EXISTING_PRINCIPAL_TRANSFORMER
                    + " doesn't occur");
        }
    }

    /**
     * Adding an "aggregate-realm" with a "principal-transformer".
     * Name of principal transformer is empty string.
     * Check that correct error message is printed.
     */
    @Test
    public void emptyPrincipalTransformer() throws Exception {
        EmptyTransformerSetupTask task = new EmptyTransformerSetupTask();
        boolean expectedErrorFound = false;
        try {
            task.setup(mClient, null);
        } catch (Exception e)  {
            String msg = e.getMessage();
            if (msg.contains("WFLYCTL0113") && msg.contains("principal-transformer")) {
                expectedErrorFound = true;
            }
        }
        if (!expectedErrorFound) {
            try {
                task.tearDown(mClient, null);
            } catch (Exception e) {
                // tearDown needs to be done as a cleanup for sure
                // originally, realm should not be created, but expectation was not fulfilled
            }
            Assert.fail("Expected error containing WFLYCTL0113 and principal-transformer doesn't occur");
        }
    }

    /**
     * Server setup task tries to prepare aggregate security realm with non-existing principal transformer
     */
    static class InvalidTransformerSetupTask extends AbstractElytronSetupTask {
        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_NAME)
                    .withAuthenticationRealm(FILESYSTEM_REALM_AUTHN_NAME)
                    .withAuthorizationRealm(FILESYSTEM_REALM_AUTHZ_NAME)
                    .withPrincipalTransformer(NON_EXISTING_PRINCIPAL_TRANSFORMER)
                    .build());
            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }
    }

    /**
     * Server setup task tries to prepare aggregate security realm with principal transformer with empty name
     */
    static class EmptyTransformerSetupTask extends AbstractElytronSetupTask {
        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_NAME)
                    .withAuthenticationRealm(FILESYSTEM_REALM_AUTHN_NAME)
                    .withAuthorizationRealm(FILESYSTEM_REALM_AUTHZ_NAME)
                    .withPrincipalTransformer(EMPTY_PRINCIPAL_TRANSFORMER)
                    .build());
            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }
    }

    /**
     * Testcase prerequisites
     * Create principal transformer, authentication and authorization realm
     */
    static class InitSetupTask extends AbstractElytronSetupTask {
        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();

            // prepare principal transcormer
            configurableElements.add(RegexPrincipalTransformer.builder(VALID_PRINCIPAL_TRANSFORMER)
                    .withPattern("a")
                    .withReplacement("b")
                    .build());

            // filesystem-realm realm for authentication
            configurableElements.add(FileSystemRealm.builder()
                    .withName(FILESYSTEM_REALM_AUTHN_NAME)
                    .build());

            // filesystem-realm realm for authorization
            configurableElements.add(FileSystemRealm.builder()
                    .withName(FILESYSTEM_REALM_AUTHZ_NAME)
                    .build());

            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }
    }
}

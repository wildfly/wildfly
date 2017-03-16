/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.security.credentialreference;

import static org.jboss.as.controller.security.CredentialReference.CREDENTIAL_REFERENCE;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;

import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * tests for credential-reference in datasource subsystem
 */
@RunWith(Arquillian.class)
@ServerSetup({CredentialStoreServerSetupTask.class, CredentialReferenceDatasourceTestCase.DatasourceServerSetupTask.class})
@RunAsClient
public class CredentialReferenceDatasourceTestCase {
    private static final String DATABASE_PASSWORD = "chucknorris";
    private static final PathAddress DATASOURCES_SUBSYSTEM_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "datasources");
    private static final String CLEAR_TEXT_CREDENTIAL_REF_DS_NAME = "ClearTextCredentialReferenceDatasource";
    private static final String STORE_ALIAS_CREDENTIAL_REF_DS_NAME = "StoreAliasCredentialReferenceDatasource";
    private static final String PASSWORD_AND_CREDENTIAL_REF_DS_NAME = "PasswordAndClearTextCredentialReferenceDatasource";

    enum Scenario {
        // <credential-reference clear-text="chucknorris"/>
        CREDENTIAL_REFERENCE_CLEAR_TEXT(CLEAR_TEXT_CREDENTIAL_REF_DS_NAME),

        // <credential-reference store="store001" alias="alias001"/>
        CREDENTIAL_REFERENCE_STORE_ALIAS(STORE_ALIAS_CREDENTIAL_REF_DS_NAME),

        // password and credential-reference should be mutually exclusive
        // test that it is not possible to create datasource with both defined
        PASSWORD_AND_CREDENTIAL_REFERENCE_PREFERENCE(PASSWORD_AND_CREDENTIAL_REF_DS_NAME);

        private final String datasourceName;

        Scenario(String datasourceName) {
            Objects.requireNonNull(datasourceName);
            this.datasourceName = datasourceName;
        }

        private PathAddress getDatasourceAddress() {
            return DATASOURCES_SUBSYSTEM_ADDRESS
                    .append("data-source", datasourceName);
        }

        private PathAddress getXADatasourceAddress() {
            return DATASOURCES_SUBSYSTEM_ADDRESS
                    .append("xa-data-source", datasourceName);
        }

        public String getDatasourceJndiName() {
            return "java:jboss/datasources/" + datasourceName;
        }
    }

    @ArquillianResource
    private ManagementClient client;


    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller-client, org.jboss.as.controller\n"), "MANIFEST.MF");
        war.addClass(CredentialStoreServerSetupTask.class);
        war.addClass(CredentialReferenceDatasourceTestCase.class);
        return war;
    }

    @Test
    public void testDatasourceClearTextCredentialReference() throws IOException, MgmtOperationException {
        final Scenario scenario = Scenario.CREDENTIAL_REFERENCE_CLEAR_TEXT;
        addDatasource(scenario);
        try {
            testConnectionInPool(scenario.getDatasourceAddress());
        } finally {
            removeDatasourceSilently(scenario);
        }
    }

    @Test
    public void testDatasourceStoreAliasCredentialReference() throws IOException, MgmtOperationException {
        final Scenario scenario = Scenario.CREDENTIAL_REFERENCE_STORE_ALIAS;
        addDatasource(scenario);
        try {
            testConnectionInPool(scenario.getDatasourceAddress());
        } finally {
            removeDatasourceSilently(scenario);
        }
    }

    @Test
    public void testDatasourceCredentialReferenceOverPasswordPreference() throws IOException {
        final Scenario scenario = Scenario.PASSWORD_AND_CREDENTIAL_REFERENCE_PREFERENCE;
        try {
            addDatasource(scenario);
            fail("It shouldn't be possible to add datasource with both, credential-reference and password, defined");
        } catch (MgmtOperationException moe) {
            // expected
        } finally {
            removeDatasourceSilently(scenario);
        }
    }

    @Test
    public void testXADatasourceClearTextCredentialReference() throws IOException, MgmtOperationException {
        final Scenario scenario = Scenario.CREDENTIAL_REFERENCE_CLEAR_TEXT;
        addXADatasource(scenario);
        try {
            testConnectionInPool(scenario.getXADatasourceAddress());
        } finally {
            removeXADatasourceSilently(scenario);
        }
    }

    @Test
    public void testXADatasourceStoreAliasCredentialReference() throws IOException, MgmtOperationException {
        final Scenario scenario = Scenario.CREDENTIAL_REFERENCE_STORE_ALIAS;
        addXADatasource(scenario);
        try {
            testConnectionInPool(scenario.getXADatasourceAddress());
        } finally {
            removeXADatasourceSilently(scenario);
        }
    }

    @Test
    public void testXADatasourceCredentialReferenceOverPasswordPreference() throws IOException {
        final Scenario scenario = Scenario.PASSWORD_AND_CREDENTIAL_REFERENCE_PREFERENCE;
        try {
            addXADatasource(scenario);
            fail("It shouldn't be possible to add datasource with both, credential-reference and password, defined");
        } catch (MgmtOperationException moe) {
            // expected
        } finally {
            removeXADatasourceSilently(scenario);
        }
    }

    private void testConnectionInPool(PathAddress datasourceAddress) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("test-connection-in-pool");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(datasourceAddress.toModelNode());
        execute(operation);
    }

    private void addDatasource(final Scenario scenario) throws IOException, MgmtOperationException {
        final ModelNode addOperation = Operations.createAddOperation(scenario.getDatasourceAddress().toModelNode());
        addOperation.get("jndi-name").set(scenario.getDatasourceJndiName());
        addOperation.get("driver-name").set("h2");
        addOperation.get("user-name").set("sa");
        addOperation.get("connection-url").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(client) + "/mem:" + CredentialReferenceDatasourceTestCase.class.getName());
        addOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);

        ModelNode credentialReference = null;
        switch (scenario) {
            case PASSWORD_AND_CREDENTIAL_REFERENCE_PREFERENCE:
                addOperation.get("password").set("wrong-password");
                credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
                credentialReference.get("clear-text").set(DATABASE_PASSWORD);
                break;
            case CREDENTIAL_REFERENCE_CLEAR_TEXT:
                credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
                credentialReference.get("clear-text").set(DATABASE_PASSWORD);
                break;
            case CREDENTIAL_REFERENCE_STORE_ALIAS:
                credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
                credentialReference.get("store").set("store001");
                credentialReference.get("alias").set("alias001");
                break;
        }
        execute(addOperation);
    }

    private void removeDatasourceSilently(final Scenario scenario) throws IOException {
        final ModelNode removeOperation = Operations.createRemoveOperation(scenario.getDatasourceAddress().toModelNode());
        removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
        try {
            execute(removeOperation);
        } catch (MgmtOperationException moe) {
            // ignore
        }
    }

    private void addXADatasource(final Scenario scenario) throws IOException, MgmtOperationException {
        final ModelNode addOperation = Operations.createAddOperation(scenario.getXADatasourceAddress().toModelNode());
        addOperation.get("jndi-name").set(scenario.getDatasourceJndiName());
        addOperation.get("driver-name").set("h2");
        addOperation.get("user-name").set("sa");
        addOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);

        ModelNode credentialReference = null;
        switch (scenario) {
            case PASSWORD_AND_CREDENTIAL_REFERENCE_PREFERENCE:
                addOperation.get("password").set("wrong-password");
                credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
                credentialReference.get("clear-text").set(DATABASE_PASSWORD);
                break;
            case CREDENTIAL_REFERENCE_CLEAR_TEXT:
                credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
                credentialReference.get("clear-text").set(DATABASE_PASSWORD);
                break;
            case CREDENTIAL_REFERENCE_STORE_ALIAS:
                credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
                credentialReference.get("store").set("store001");
                credentialReference.get("alias").set("alias001");
                break;
        }

        final PathAddress urlXADatasourcePropertyAddress = scenario.getXADatasourceAddress().append("xa-datasource-properties", "URL");
        final ModelNode addURLDatasourcePropertyOperation = Operations.createAddOperation(urlXADatasourcePropertyAddress.toModelNode());
        addURLDatasourcePropertyOperation.get("value").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(client) + "/mem:" + CredentialReferenceDatasourceTestCase.class.getName());

        ModelNode compositeOperation = Operations.createCompositeOperation();
        compositeOperation.get(ModelDescriptionConstants.STEPS).add(addOperation);
        compositeOperation.get(ModelDescriptionConstants.STEPS).add(addURLDatasourcePropertyOperation);

        execute(compositeOperation);
    }

    private void removeXADatasourceSilently(final Scenario scenario) throws IOException {
        final ModelNode removeOperation = Operations.createRemoveOperation(scenario.getXADatasourceAddress().toModelNode());
        removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
        try {
            execute(removeOperation);
        } catch (MgmtOperationException moe) {
            // ignore
        }
    }

    private ModelNode execute(final ModelNode op) throws IOException, MgmtOperationException {
        final ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new MgmtOperationException(Operations.getFailureDescription(result).asString());
        }
        return result;
    }

    public static class DatasourceServerSetupTask implements ServerSetupTask {

        private Server h2Server;
        private Connection connection;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            h2Server = Server.createTcpServer("-tcpAllowOthers").start();
            // open connection to database, because that's only (easy) way to set password for user sa
            connection = DriverManager.getConnection("jdbc:h2:mem:" + CredentialReferenceDatasourceTestCase.class.getName() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "sa", DATABASE_PASSWORD);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            connection.close();
            h2Server.shutdown();
        }
    }
}

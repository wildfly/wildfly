/*
 *
 * Copyright 2017 Red Hat, Inc.
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
 *
 */

package org.jboss.as.test.integration.jca.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.MatchRules;
import org.wildfly.test.security.common.elytron.SimpleAuthConfig;
import org.wildfly.test.security.common.elytron.SimpleAuthContext;

/**
 * test multiple datasources, some uses elytron and some legacy security
 */
@RunWith(Arquillian.class)
@ServerSetup({DsWithMixedSecurityTestCase.ElytronSetup.class, DsWithMixedSecurityTestCase.DsWithSecurityDomainTestCaseSetup.class, DsWithMixedSecurityTestCase.SetupDatasources.class})
public class DsWithMixedSecurityTestCase {

    private static final String AUTH_CONTEXT = "MyAuthContext";
    private static final String REALM = "DsRealm";
    private static final String LEGACY_SECURITY_DATASOURCE_NAME = "LegacySecurityDatasource";
    private static final String ELYTRON_SECURITY_DATASOURCE_NAME = "ElytronSecurityDatasource";
    private static final String LEGACY_SECURITY_XADATASOURCE_NAME = "LegacySecurityXADatasource";
    private static final String ELYTRON_SECURITY_XADATASOURCE_NAME = "ElytronSecurityXADatasource";

    static class ElytronSetup extends AbstractElytronSetupTask {
        private static final String AUTH_CONFIG = "MyAuthConfig";
        private static final String DATABASE_USER = DsWithMixedSecurityTestCase.class.getName();
        private static final String DATABASE_PASSWORD = "passWD12#$";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            final CredentialReference credRefPwd = CredentialReference.builder().withClearText(DATABASE_PASSWORD).build();
            final ConfigurableElement authenticationConfiguration = SimpleAuthConfig.builder().withName(AUTH_CONFIG)
                    .withAuthenticationName(DATABASE_USER).withCredentialReference(credRefPwd).build();
            final MatchRules matchRules = MatchRules.builder().withAuthenticationConfiguration(AUTH_CONFIG).build();
            final ConfigurableElement authenticationContext = SimpleAuthContext.builder().withName(AUTH_CONTEXT).
                    withMatchRules(matchRules).build();

            return new ConfigurableElement[]{authenticationConfiguration, authenticationContext};
        }
    }

    static class DsWithSecurityDomainTestCaseSetup extends AbstractLoginModuleSecurityDomainTestCaseSetup {

        @Override
        protected String getSecurityDomainName() {
            return REALM;
        }

        @Override
        protected String getLoginModuleName() {
            return "ConfiguredIdentity";
        }

        @Override
        protected boolean isRequired() {
            return true;
        }

        @Override
        protected Map<String, String> getModuleOptions() {
            Map<String, String> moduleOptions = new HashMap<>();
            moduleOptions.put("userName", DsWithMixedSecurityTestCase.class.getName());
            moduleOptions.put("password", "passWD12#$");
            moduleOptions.put("principal", DsWithMixedSecurityTestCase.class.getName());
            return moduleOptions;
        }
    }

    static class SetupDatasources implements ServerSetupTask {
        private static final PathAddress DATASOURCES_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "datasources");

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient mcc = managementClient.getControllerClient();
            addDatasource(LEGACY_SECURITY_DATASOURCE_NAME, addOperation -> {
                addOperation.get("security-domain").set(REALM);
            }, mcc);
            addDatasource(ELYTRON_SECURITY_DATASOURCE_NAME, addOperation -> {
                addOperation.get("elytron-enabled").set("true");
                addOperation.get("authentication-context").set(AUTH_CONTEXT);
            }, mcc);
            addXaDatasource(LEGACY_SECURITY_XADATASOURCE_NAME, addOperation -> {
                addOperation.get("security-domain").set(REALM);
            }, mcc);
            addXaDatasource(ELYTRON_SECURITY_XADATASOURCE_NAME, addOperation -> {
                addOperation.get("elytron-enabled").set("true");
                addOperation.get("authentication-context").set(AUTH_CONTEXT);
            }, mcc);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient mcc = managementClient.getControllerClient();
            removeDatasourceSilently(LEGACY_SECURITY_DATASOURCE_NAME, mcc);
            removeDatasourceSilently(ELYTRON_SECURITY_DATASOURCE_NAME, mcc);
            removeXaDatasourceSilently(LEGACY_SECURITY_XADATASOURCE_NAME, mcc);
            removeXaDatasourceSilently(ELYTRON_SECURITY_XADATASOURCE_NAME, mcc);
        }

        private void addDatasource(final String name, Consumer<ModelNode> configurationApplier, ModelControllerClient client) throws IOException {
            PathAddress addr = DATASOURCES_ADDRESS.append("data-source", name);
            final ModelNode addOperation = Operations.createAddOperation(addr.toModelNode());
            addOperation.get("jndi-name").set("java:jboss/datasources/" + name);
            addOperation.get("driver-name").set("h2");
            addOperation.get("connection-url").set("jdbc:h2:mem:" + DsWithMixedSecurityTestCase.class.getName() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            addOperation.get(OPERATION_HEADERS).get("allow-resource-service-restart").set(true);

            configurationApplier.accept(addOperation);

            ModelNode response = execute(addOperation, client);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
        }

        private void addXaDatasource(final String name, Consumer<ModelNode> configurationApplier, ModelControllerClient client) throws IOException {
            PathAddress addr = DATASOURCES_ADDRESS.append("xa-data-source", name);
            final ModelNode addOperation = Operations.createAddOperation(addr.toModelNode());
            addOperation.get("jndi-name").set("java:jboss/xa-datasources/" + name);
            addOperation.get("driver-name").set("h2");
            addOperation.get(OPERATION_HEADERS).get("allow-resource-service-restart").set(true);

            configurationApplier.accept(addOperation);

            final PathAddress urlXADatasourcePropertyAddress = addr.append("xa-datasource-properties", "URL");
            final ModelNode addURLDatasourcePropertyOperation = Operations.createAddOperation(urlXADatasourcePropertyAddress.toModelNode());
            addURLDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:" + DsWithMixedSecurityTestCase.class.getName() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

            ModelNode compositeOperation = Operations.createCompositeOperation();
            compositeOperation.get(ModelDescriptionConstants.STEPS).add(addOperation);
            compositeOperation.get(ModelDescriptionConstants.STEPS).add(addURLDatasourcePropertyOperation);

            ModelNode response = execute(compositeOperation, client);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
        }

        private void removeDatasourceSilently(final String name, ModelControllerClient client) throws IOException {
            PathAddress addr = DATASOURCES_ADDRESS.append("data-source", name);
            ModelNode removeRaOperation = Operations.createRemoveOperation(addr.toModelNode());
            removeRaOperation.get(ClientConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set("true");
            client.execute(removeRaOperation);
        }

        private void removeXaDatasourceSilently(final String name, ModelControllerClient client) throws IOException {
            PathAddress addr = DATASOURCES_ADDRESS.append("xa-data-source", name);
            ModelNode removeRaOperation = Operations.createRemoveOperation(addr.toModelNode());
            removeRaOperation.get(ClientConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set("true");
            client.execute(removeRaOperation);
        }

        private ModelNode execute(ModelNode operation, ModelControllerClient client) throws IOException {
            return client.execute(operation);
        }
    }

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar").addClasses(DsWithMixedSecurityTestCase.class);
        jar.addClasses(AbstractElytronSetupTask.class, AbstractLoginModuleSecurityDomainTestCaseSetup.class, AbstractSecurityDomainSetup.class);
        return ShrinkWrap.create(EnterpriseArchive.class, "test.ear").addAsLibrary(jar);
    }

    @Resource(mappedName = "java:jboss/datasources/" + LEGACY_SECURITY_DATASOURCE_NAME)
    private DataSource legacySecurityDs;

    @Resource(mappedName = "java:jboss/datasources/" + ELYTRON_SECURITY_DATASOURCE_NAME)
    private DataSource elytronSecurityDs;

    @Resource(mappedName = "java:jboss/xa-datasources/" + LEGACY_SECURITY_XADATASOURCE_NAME)
    private DataSource legacySecurityXaDs;

    @Resource(mappedName = "java:jboss/xa-datasources/" + ELYTRON_SECURITY_XADATASOURCE_NAME)
    private DataSource elytronSecurityXaDs;

    @Test
    public void testLegacySecurityDatasource() throws Exception {
        testDatasource(legacySecurityDs);
    }

    @Test
    public void testElytronSecurityDatasource() throws Exception {
        testDatasource(elytronSecurityDs);
    }

    @Test
    public void testLegacySecurityXaDatasource() throws Exception {
        testDatasource(legacySecurityXaDs);
    }

    @Test
    public void testElytronSecurityXaDatasource() throws Exception {
        testDatasource(elytronSecurityXaDs);
    }

    private void testDatasource(DataSource ds) throws Exception {
        Assert.assertNotNull(ds);
        try (Connection con = ds.getConnection()) {
            assertNotNull(con);
        }
    }


}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Resource;
import javax.resource.cci.Connection;
import javax.security.auth.PrivateCredentialPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
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
 * test RA that has two connection definitions, one is using legacy security and second elytron
 */
@RunWith(Arquillian.class)
@ServerSetup({WildFlyActivationRaWithMixedSecurityTestCase.ElytronSetup.class, WildFlyActivationRaWithMixedSecurityTestCase.SecurityDomainSetup.class,
        WildFlyActivationRaWithMixedSecurityTestCase.RaSetup.class})
public class WildFlyActivationRaWithMixedSecurityTestCase {


    private static final String AUTH_CONTEXT = "MyAuthContext";
    private static final String LEGACY_SECURITY_CONN_DEF_JNDI_NAME = "java:jboss/wf-ra-security-domain";
    private static final String ELYTRON_SECURITY_CONN_DEF_JNDI_NAME = "java:jboss/wf-ra-elytron-security";
    private static final String SECURITY_REALM_NAME = "RaRealm";

    static class ElytronSetup extends AbstractElytronSetupTask {
        private static final String AUTH_CONFIG = "MyAuthConfig";
        private static final String CREDENTIAL = "sa";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            final CredentialReference credRefPwd = CredentialReference.builder().withClearText(CREDENTIAL).build();
            final ConfigurableElement authenticationConfiguration = SimpleAuthConfig.builder().withName(AUTH_CONFIG)
                    .withAuthenticationName(CREDENTIAL).withCredentialReference(credRefPwd).build();
            final MatchRules matchRules = MatchRules.builder().withAuthenticationConfiguration(AUTH_CONFIG).build();
            final ConfigurableElement authenticationContext = SimpleAuthContext.builder().withName(AUTH_CONTEXT).
                    withMatchRules(matchRules).build();

            return new ConfigurableElement[]{authenticationConfiguration, authenticationContext};
        }
    }

    static class SecurityDomainSetup extends AbstractLoginModuleSecurityDomainTestCaseSetup {

        @Override
        protected String getSecurityDomainName() {
            return SECURITY_REALM_NAME;
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
            moduleOptions.put("userName", "sa");
            moduleOptions.put("password", "sa");
            moduleOptions.put("principal", "sa");
            return moduleOptions;
        }
    }

    static class RaSetup implements ServerSetupTask {
        private static final PathAddress RA_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "resource-adapters")
                .append("resource-adapter", "wf-ra-elytron-security");

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient mcc = managementClient.getControllerClient();
            addResourceAdapter(mcc);
            addConnectionDefinition("pool1", ELYTRON_SECURITY_CONN_DEF_JNDI_NAME, addConnectionDefinitionOperation -> {
                addConnectionDefinitionOperation.get("elytron-enabled").set("true");
                addConnectionDefinitionOperation.get("authentication-context").set(AUTH_CONTEXT);
            }, mcc);
            addConnectionDefinition("pool2", LEGACY_SECURITY_CONN_DEF_JNDI_NAME, addConnectionDefinitionOperation -> {
                addConnectionDefinitionOperation.get("security-domain").set(SECURITY_REALM_NAME);
            }, mcc);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            removeResourceAdapterSilently(managementClient.getControllerClient());
        }

        private void addResourceAdapter(ModelControllerClient client) throws IOException {
            ModelNode addRaOperation = Operations.createAddOperation(RA_ADDRESS.toModelNode());
            addRaOperation.get("archive").set("wf-ra-ely-security.rar");
            addRaOperation.get("transaction-support").set("NoTransaction");
            ModelNode response = execute(addRaOperation, client);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
        }

        private void addConnectionDefinition(String name, String jndiName, Consumer<ModelNode> attrProvider, ModelControllerClient client) throws IOException {
            PathAddress connectionDefinitionAddress = RA_ADDRESS.append("connection-definitions", name);
            ModelNode addConnectionDefinitionOperation = Operations.createAddOperation(connectionDefinitionAddress.toModelNode());
            addConnectionDefinitionOperation.get("class-name").set("org.jboss.as.test.integration.jca.rar.MultipleManagedConnectionFactoryWithSubjectVerification");
            addConnectionDefinitionOperation.get("jndi-name").set(jndiName);

            attrProvider.accept(addConnectionDefinitionOperation);

            ModelNode response = execute(addConnectionDefinitionOperation, client);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
        }

        private void removeResourceAdapterSilently(ModelControllerClient client) throws IOException {
            ModelNode removeRaOperation = Operations.createRemoveOperation(RA_ADDRESS.toModelNode());
            removeRaOperation.get(ClientConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set("true");
            client.execute(removeRaOperation);
        }

        private ModelNode execute(ModelNode operation, ModelControllerClient client) throws IOException {
            return client.execute(operation);
        }
    }

    @Deployment(name = "wf-ra-ely-security", testable = false, order = 1)
    public static Archive<?> deployElytronRa() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage());
        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "wf-ra-ely-security.rar")
                .addAsLibrary(jar)
                .addAsManifestResource(WildFlyActivationRaWithMixedSecurityTestCase.class.getPackage(), "ra.xml", "ra.xml");
        rar.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                new PrivateCredentialPermission(
                        "javax.resource.spi.security.PasswordCredential org.wildfly.security.auth.principal.NamePrincipal \"sa\"", "read"),
                new PrivateCredentialPermission(
                        "javax.resource.spi.security.PasswordCredential org.jboss.security.SimplePrincipal \"sa\"", "read")),
                "permissions.xml");

        return rar;
    }

    @Deployment(name = "ear", order = 2)
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar");
        jar.addClasses(AbstractElytronSetupTask.class, WildFlyActivationRaWithMixedSecurityTestCase.class,
                AbstractLoginModuleSecurityDomainTestCaseSetup.class, AbstractSecurityDomainSetup.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear").addAsLibrary(jar)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller, org.jboss.as.controller-client, deployment.wf-ra-ely-security.rar\n"), "MANIFEST.MF");
        ear.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                new PrivateCredentialPermission(
                        "javax.resource.spi.security.PasswordCredential org.wildfly.security.auth.principal.NamePrincipal \"sa\"", "read"),
                new PrivateCredentialPermission(
                        "javax.resource.spi.security.PasswordCredential org.jboss.security.SimplePrincipal \"sa\"", "read")),
                "permissions.xml");
        return ear;
    }

    @Resource(mappedName = LEGACY_SECURITY_CONN_DEF_JNDI_NAME)
    private MultipleConnectionFactory1 legacySecurityConnectionFactory;

    @Resource(mappedName = ELYTRON_SECURITY_CONN_DEF_JNDI_NAME)
    private MultipleConnectionFactory1 elytronSecurityConnectionFactory;

    @Test
    @OperateOnDeployment("ear")
    public void testLegacySecurityConnectionFactory() throws Exception {
        testConnectionFactory(legacySecurityConnectionFactory);
    }

    @Test
    @OperateOnDeployment("ear")
    public void testElytronSecurityConnectionFactory() throws Exception {
        testConnectionFactory(elytronSecurityConnectionFactory);
    }

    public void testConnectionFactory(MultipleConnectionFactory1 mcf) throws Exception {
        assertNotNull("CF not found", mcf);
        Connection cci = mcf.getConnection();
        assertNotNull("Cannot obtain connection", cci);
        cci.close();
    }
}

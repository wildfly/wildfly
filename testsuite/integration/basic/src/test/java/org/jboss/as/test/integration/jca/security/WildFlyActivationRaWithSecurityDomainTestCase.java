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
import javax.annotation.Resource;
import javax.resource.cci.Connection;
import javax.security.auth.PrivateCredentialPermission;

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
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for RA with security domain, RA is activated using resource-adapter subsystem
 */
@RunWith(Arquillian.class)
@ServerSetup({WildFlyActivationRaWithSecurityDomainTestCase.RaWithSecurityDomainTestCaseSetup.class, WildFlyActivationRaWithSecurityDomainTestCase.RaSetup.class})
public class WildFlyActivationRaWithSecurityDomainTestCase {
    private static final String CONN_DEF_JNDI_NAME = "java:jboss/wf-ra-security-domain";

    static class RaWithSecurityDomainTestCaseSetup extends AbstractLoginModuleSecurityDomainTestCaseSetup {

        @Override
        protected String getSecurityDomainName() {
            return "RaRealm";
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
            Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put("userName", "sa");
            moduleOptions.put("password", "sa");
            moduleOptions.put("principal", "sa");
            return moduleOptions;
        }
    }

    static class RaSetup implements ServerSetupTask {
        private static final PathAddress RA_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "resource-adapters")
                .append("resource-adapter", "wf-ra-security-domain");

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient mcc = managementClient.getControllerClient();
            addResourceAdapter(mcc);
            addConnectionDefinition(mcc);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            removeResourceAdapterSilently(managementClient.getControllerClient());
        }

        private void addResourceAdapter(ModelControllerClient client) throws IOException {
            ModelNode addRaOperation = Operations.createAddOperation(RA_ADDRESS.toModelNode());
            addRaOperation.get("archive").set("wf-ra-security-domain.rar");
            addRaOperation.get("transaction-support").set("NoTransaction");
            ModelNode response = execute(addRaOperation, client);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
        }

        private void addConnectionDefinition(ModelControllerClient client) throws IOException {
            PathAddress connectionDefinitionAddress = RA_ADDRESS.append("connection-definitions", "Pool1");
            ModelNode addConnectionDefinitionOperation = Operations.createAddOperation(connectionDefinitionAddress.toModelNode());
            addConnectionDefinitionOperation.get("class-name").set("org.jboss.as.test.integration.jca.rar.MultipleManagedConnectionFactoryWithSubjectVerification");
            addConnectionDefinitionOperation.get("jndi-name").set(CONN_DEF_JNDI_NAME);
            addConnectionDefinitionOperation.get("security-domain").set("RaRealm");

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

    @Deployment
    public static Archive<?> deploymentSingleton() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar").addClass(WildFlyActivationRaWithSecurityDomainTestCase.class)
                .addPackage(MultipleConnectionFactory1.class.getPackage());
        jar.addClasses(AbstractLoginModuleSecurityDomainTestCaseSetup.class, AbstractSecurityDomainSetup.class);
        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "wf-ra-security-domain.rar").addAsLibrary(jar)
                .addAsManifestResource(WildFlyActivationRaWithSecurityDomainTestCase.class.getPackage(), "ra.xml", "ra.xml");
        rar.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                new PrivateCredentialPermission("javax.resource.spi.security.PasswordCredential org.jboss.security.SimplePrincipal \"sa\"", "read")), "permissions.xml");

        return rar;
    }

    @Resource(mappedName = CONN_DEF_JNDI_NAME)
    private MultipleConnectionFactory1 connectionFactory1;

    @Test
    public void deploymentTest() throws Exception {
        assertNotNull("CF1 not found", connectionFactory1);
        Connection cci = connectionFactory1.getConnection();
        assertNotNull("Cannot obtain connection", cci);
        cci.close();
    }
}

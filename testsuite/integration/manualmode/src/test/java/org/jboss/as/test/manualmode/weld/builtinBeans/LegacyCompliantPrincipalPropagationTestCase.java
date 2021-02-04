/*
 * Copyright 2021 Red Hat, Inc.
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
package org.jboss.as.test.manualmode.weld.builtinBeans;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.client.helpers.Operations.createAddOperation;
import static org.jboss.as.controller.client.helpers.Operations.createRemoveOperation;
import static org.jboss.as.controller.client.helpers.Operations.createWriteAttributeOperation;
import static org.junit.Assume.assumeTrue;


/**
 * Functionality tests for legacy-compliant-principal-propagation attribute of EJB3 subsystem.
 * See <a href="https://issues.jboss.org/browse/WFLY-11587">WFLY-14074</a>.
 */
@RunWith(Arquillian.class)
public class LegacyCompliantPrincipalPropagationTestCase {

    private static final String ANONYMOUS_PRINCIPAL = "anonymous";
    private static final String NON_ANONYMOUS_PRINCIPAL = "non-anonymous";
    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";
    private static final String LEGACY_COMPLIANT_ATTRIBUTE_TEST_DEPL = "legacy-compliant-test-deployment";

    @ArquillianResource
    private static Deployer deployer;

    @ArquillianResource
    private static ContainerController serverController;

    @Deployment(name = LEGACY_COMPLIANT_ATTRIBUTE_TEST_DEPL, managed = false)
    @TargetsContainer(DEFAULT_FULL_JBOSSAS)
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class).addPackage(LegacyCompliantPrincipalPropagationTestCase.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(LegacyCompliantPrincipalPropagationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    @BeforeClass
    public static void executeOnlyForElytron() {
        // this functionality affects elytron only
        assumeTrue(System.getProperty("elytron") != null);
    }

    @Test
    @InSequence(1)
    @RunAsClient
    public void startContainerAndConfigureApplicationSecDomain() throws Exception {
        if (!serverController.isStarted(DEFAULT_FULL_JBOSSAS)) {
            serverController.start(DEFAULT_FULL_JBOSSAS);
        }
        // add application-security-domain called "other" that is mapped with an Elytron security domain (ApplicationDomain) in the EJB subsystem
        ManagementClient managementClient = getManagementClient();
        ModelNode modelNode = createAddOperation(PathAddress.parseCLIStyleAddress(("/subsystem=ejb3/application-security-domain=other")).toModelNode());
        modelNode.get("security-domain").set("ApplicationDomain");
        managementClient.getControllerClient().execute(modelNode);

        ServerReload.reloadIfRequired(managementClient);
        deployer.deploy(LEGACY_COMPLIANT_ATTRIBUTE_TEST_DEPL);
    }

    @Test
    @InSequence(2)
    public void testPrincipalUnsecuredLocalEJBInLegacyCompliantMode(CallerWithIdentity callerWithIdentity) {
        Assert.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalFromEJBContext());
    }

    @Test
    @InSequence(3)
    @RunAsClient
    public void configureLegacyIncompatibleMode() throws Exception {
        // deployer needs to redeploy for Arquillian to find config for this class to run test methods
        deployer.undeploy(LEGACY_COMPLIANT_ATTRIBUTE_TEST_DEPL);
        ManagementClient managementClient = getManagementClient();
        ModelNode modelNode = createWriteAttributeOperation(PathAddress.parseCLIStyleAddress(("/subsystem=ejb3/application-security-domain=other")).toModelNode(),
                "legacy-compliant-principal-propagation", false);
        managementClient.getControllerClient().execute(modelNode);

        // must reload after changing attribute's value
        ServerReload.reloadIfRequired(managementClient);
        deployer.deploy(LEGACY_COMPLIANT_ATTRIBUTE_TEST_DEPL);
    }

    @Test
    @InSequence(4)
    public void testPrincipalUnsecuredLocalEJBInElytronMode(CallerWithIdentity callerWithIdentity) {
        Assert.assertEquals(ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalFromEJBContext());
    }

    @Test
    @InSequence(5)
    @RunAsClient
    public void restoreConfigurationAndStopContainer() throws Exception {
        deployer.undeploy(LEGACY_COMPLIANT_ATTRIBUTE_TEST_DEPL);

        ModelNode modelNode = createRemoveOperation(PathAddress.parseCLIStyleAddress(("/subsystem=ejb3/application-security-domain=other")).toModelNode());
        getManagementClient().getControllerClient().execute(modelNode);

        serverController.stop(DEFAULT_FULL_JBOSSAS);
    }

    private ManagementClient getManagementClient() {
        ModelControllerClient modelControllerClient = TestSuiteEnvironment.getModelControllerClient();
        return new ManagementClient(modelControllerClient, TestSuiteEnvironment.getServerAddress(), 9990, "remote+http");
    }
}

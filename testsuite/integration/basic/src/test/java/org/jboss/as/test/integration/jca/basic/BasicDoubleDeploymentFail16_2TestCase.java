/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.basic;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import jakarta.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test failure of activation for non ID-ed RAs
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@ServerSetup(BasicDoubleDeploymentFail16_2TestCase.BasicDoubleDeploymentTestCaseSetup.class)
public class BasicDoubleDeploymentFail16_2TestCase {

    // deployment archive name must match "archive" element.
    private static final String DEPLOYMENT_MODULE_NAME = "basic";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_MODULE_NAME + ".rar";
    private static final String SUB_DEPLOYMENT_MODULE_NAME = "somejar";
    private static final String SUB_DEPLOYMENT_NAME = SUB_DEPLOYMENT_MODULE_NAME + ".jar";
    private static final String RAR_1_NAME = "XXX2";

    // private static final String RAR_2_NAME = "XXX2";

    static class BasicDoubleDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            String xml = FileUtils.readFile(BasicDoubleDeploymentFail16_2TestCase.class, "basic16.1-2.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_1.getUriString(),
                    new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations));
            xml = FileUtils.readFile(BasicDoubleDeploymentFail16_2TestCase.class, "basic16.1-3.xml");
            operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_1.getUriString(),
                    new ResourceAdapterSubsystemParser());
            final ModelNode result = executeOperation(operationListToCompositeOperation(operations), false);
            Assert.assertTrue(!Operations.isSuccessfulOutcome(result));
            final String failureDescription = result.get("result").get("step-1").get("failure-description").asString();
            Assert.assertTrue(failureDescription.startsWith("WFLYCTL0212: Duplicate resource"));

        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            try {
                remove(getAddress(RAR_1_NAME));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private ModelNode getAddress(final String name) {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", name);
            address.protect();
            return address;
        }
    }

    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, DEPLOYMENT_NAME);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, SUB_DEPLOYMENT_NAME);
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).addClasses(BasicDoubleDeploymentFail16_2TestCase.class,
                BasicDoubleDeploymentTestCaseSetup.class);
        ja.addPackage(AbstractMgmtTestBase.class.getPackage()); // needed to process the @ServerSetup annotation on the server side

        raa.addAsLibrary(ja);

        raa.addAsManifestResource(BasicDoubleDeploymentFail16_2TestCase.class.getPackage(), "ra16.xml", "ra.xml");
        return raa;
    }

    @Resource(mappedName = "java:jboss/name1-2")
    private MultipleConnectionFactory1 connectionFactory1;

    @Resource(mappedName = "java:jboss/Name3-2")
    private MultipleAdminObject1 adminObject1;

    @ContainerResource
    private InitialContext initialContext;

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        assertNotNull("CF1 not found", connectionFactory1);
        assertNotNull("AO1 not found", adminObject1);
    }

    @Test(expected = NameNotFoundException.class)
    public void testNonExistingConfig_1() throws Exception {
        InitialContext initialContext = new InitialContext();
        initialContext.lookup("java:jboss/name1-3");
    }

    @Test(expected = NameNotFoundException.class)
    public void testNonExistingConfig_2() throws Exception {
        InitialContext initialContext = new InitialContext();
        initialContext.lookup("java:jboss/Name3-3");
    }
}

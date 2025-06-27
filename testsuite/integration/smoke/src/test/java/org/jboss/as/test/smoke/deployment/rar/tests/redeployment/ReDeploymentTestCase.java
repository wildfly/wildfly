/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.tests.redeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5968 test for undeployment and re-deployment
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@ServerSetup(ReDeploymentTestCase.ReDeploymentTestCaseSetup.class)
public class ReDeploymentTestCase extends ContainerResourceMgmtTestBase {

    static String deploymentName = "re-deployment.rar";

    @ContainerResource
    private Context context;

    @ArquillianResource
    private Deployer deployer;

    static class ReDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", deploymentName);
            address.protect();
            remove(address);
        }

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {

        }
    }

    private void setup() throws Exception {
        String xml = FileUtils.readFile(ReDeploymentTestCase.class, "re-deployment.xml");
        List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_0.getUriString(), new ResourceAdapterSubsystemParser());
        executeOperation(operationListToCompositeOperation(operations));

        //since it is created after deployment it needs activation
        final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter", deploymentName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);

    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment(name = "re-deployment.rar", managed = false)
    public static ResourceAdapterArchive createDeployment() throws Exception {


        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(ReDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml");
        return raa;
    }

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        deployer.deploy(deploymentName);
        setup();
        deployer.undeploy(deploymentName);
        deployer.deploy(deploymentName);
        MultipleAdminObject1 adminObject1 = (MultipleAdminObject1) context.lookup("redeployed/Name3");
        assertNotNull(adminObject1, "AO1 not found");
        deployer.undeploy(deploymentName);
    }

}

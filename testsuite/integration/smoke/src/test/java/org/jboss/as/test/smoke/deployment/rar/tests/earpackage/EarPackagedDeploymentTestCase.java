/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.tests.earpackage;

import java.util.List;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="robert.reimann@googlemail.com">Robert Reimann</a>
 *         Deployment of a RAR packaged inside an EAR.
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(EarPackagedDeploymentTestCase.EarPackagedDeploymentTestCaseSetup.class)
public class EarPackagedDeploymentTestCase {

    static class EarPackagedDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            String xml = FileUtils.readFile(EarPackagedDeploymentTestCase.class, "ear_packaged.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_0.getUriString(), new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", "ear_packaged.ear#ear_packaged.rar");
            address.protect();
            remove(address);
        }
    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static EnterpriseArchive createDeployment() {

        String deploymentName = "ear_packaged.ear";
        String subDeploymentName = "ear_packaged.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, subDeploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).
                addClasses(EarPackagedDeploymentTestCase.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());  // needed to process the @ServerSetup annotation on the server side
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(EarPackagedDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        ear.addAsModule(raa);
        ear.addAsManifestResource(EarPackagedDeploymentTestCase.class.getPackage(), "application.xml", "application.xml");
        return ear;
    }

    @Resource(mappedName = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;


    @Resource(mappedName = "java:jboss/Name3")
    private MultipleAdminObject1 adminObject1;


    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {

        assertNotNull(connectionFactory1, "CF1 not found");
        assertNotNull(adminObject1, "AO1 not found");
    }
}

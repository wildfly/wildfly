/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.ijdeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidResourceAdapter;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of two ear ij deployments conflict.
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IronJacamarDoubleDeploymentTestCase extends ContainerResourceMgmtTestBase {
    private static final String deploymentName = "test-ij.ear";
    private static final String deployment2Name = "test-ij2.ear";
    private static final String deploymentConfigName = "ironjacamar.xml";
    private static final String deployment2ConfigName = "ironjacamar-2.xml";

    private static final String subDeploymentName = "ij.rar";

    private static ResourceAdapterArchive createSubDeployment(final String configName) throws Exception {
        String deploymentName = subDeploymentName;

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "ij.jar");
        ja.addPackage(ValidResourceAdapter.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(IronJacamarDoubleDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(IronJacamarDoubleDeploymentTestCase.class.getPackage(), configName, "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: javax.inject.api,org.jboss.as.connector\n"),
                        "MANIFEST.MF");
        return raa;
    }

    @Deployment(name = deploymentName)
    public static EnterpriseArchive createEARDeployment() throws Exception {

        ResourceAdapterArchive raa = createSubDeployment(deploymentConfigName);

        EnterpriseArchive earTest = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        earTest.addAsManifestResource(IronJacamarDoubleDeploymentTestCase.class.getPackage(), "application.xml",
                "application.xml");
        earTest.addAsModule(raa);
        return earTest;
    }

    @Deployment(name = deployment2Name)
    public static EnterpriseArchive createEAR2Deployment() throws Exception {

        ResourceAdapterArchive raa = createSubDeployment(deployment2ConfigName);

        EnterpriseArchive earTest = ShrinkWrap.create(EnterpriseArchive.class, deployment2Name);
        earTest.addAsManifestResource(IronJacamarDoubleDeploymentTestCase.class.getPackage(), "application.xml",
                "application.xml");
        earTest.addAsModule(raa);
        return earTest;
    }

    /**
     *
     */
    @Test
    public void testEarConfiguration() throws Throwable {
        ModelNode address = getAddress(deploymentName);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");
        operation.get(OP_ADDR).set(address);
        operation.get(RECURSIVE).set(true);
        executeOperation(operation);

        address = getAddress(deployment2Name);
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);

    }

    private ModelNode getAddress(String deploymentName) {
        final ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, deploymentName).add(SUBDEPLOYMENT, subDeploymentName).add(SUBSYSTEM, "resource-adapters")
                .add("ironjacamar", "ironjacamar").add("resource-adapter", deploymentName + "#ij.rar");
        address.protect();
        return address;
    }

}

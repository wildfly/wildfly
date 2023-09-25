/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.ear;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;

import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a> JBQA-5968 test
 *         for undeployment and re-deployment
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RarInsideEarReDeploymentTestCase extends
        ContainerResourceMgmtTestBase {

    static final String deploymentName = "re-deployment.ear";
    static String subDeploymentName = "ear_packaged.rar";

    private static ModelNode address;

    @ContainerResource
    private Context context;

    @ArquillianResource
    private Deployer deployer;

    private void setup() throws Exception {

        // since it is created after deployment it needs activation
        address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter", deploymentName + "#"
                + subDeploymentName);
        address.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("archive").set(deploymentName + "#" + subDeploymentName);
        executeOperation(operation);

        ModelNode addr = address.clone();
        addr.add("admin-objects", "Pool3");

        operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(addr);
        operation.get("jndi-name").set("java:jboss/exported/redeployed/Name3");
        operation
                .get("class-name")
                .set("org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl");
        executeOperation(operation);

        operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);

    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment(name = deploymentName, managed = false)
    public static EnterpriseArchive createDeployment() throws Exception {

        ResourceAdapterArchive raa = ShrinkWrap.create(
                ResourceAdapterArchive.class, subDeploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleAdminObject1.class.getPackage()).addClasses(
                RarInsideEarReDeploymentTestCase.class,
                MgmtOperationException.class, XMLElementReader.class,
                XMLElementWriter.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(
                RarInsideEarReDeploymentTestCase.class.getPackage(), "ra.xml",
                "ra.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr\n"),
                        "MANIFEST.MF");

        final EnterpriseArchive ear = ShrinkWrap.create(
                EnterpriseArchive.class, deploymentName);
        ear.addAsModule(raa);
        ear.addAsManifestResource(
                RarInsideEarReDeploymentTestCase.class.getPackage(),
                "application.xml", "application.xml");
        return ear;
    }

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        try {
            deployer.deploy(deploymentName);
            setup();
            deployer.undeploy(deploymentName);
            deployer.deploy(deploymentName);
            MultipleAdminObject1 adminObject1 = (MultipleAdminObject1) context
                    .lookup("redeployed/Name3");
            assertNotNull("AO1 not found", adminObject1);
        } finally {
            deployer.undeploy(deploymentName);
            remove(address);
        }
    }

}

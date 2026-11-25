/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.tests.eardeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.smoke.deployment.RaServlet;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5828 RAR inside EAR
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class EarDeploymentTestCase extends ContainerResourceMgmtTestBase {

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    static String subdeploymentName = "complex_ij.rar";
    static String deploymentName = "new.ear";


    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {
        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, subdeploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage());
        raa.addAsManifestResource(EarDeploymentTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(EarDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,javax.inject.api,org.jboss.as.connector\n"), "MANIFEST.MF");

        WebArchive wa = ShrinkWrap.create(WebArchive.class, "servlet.war");
        wa.addClasses(RaServlet.class);

        EnterpriseArchive ea = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        ea.addAsModule(raa)
                .addAsModule(wa)
                .addAsLibrary(ja);
        return ea;
    }

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testWebConfiguration() throws Throwable {
        URL servletURL = new URL("http://" + url.getHost() + ":8080/servlet" + RaServlet.URL_PATTERN);
        BufferedReader br = new BufferedReader(new InputStreamReader(servletURL.openStream(), StandardCharsets.UTF_8));
        String message = br.readLine();
        assertEquals(RaServlet.SUCCESS, message);
    }

    @Test
    public void testConfiguration() throws Throwable {
        assertNotNull(managementClient.getProtocolMetaData(deploymentName), "Deployment metadata for ear not found");

        final ModelNode address = new ModelNode();
        address.add("deployment", deploymentName).add("subdeployment", subdeploymentName).add("subsystem", "resource-adapters");
        address.protect();
        final ModelNode snapshot = new ModelNode();
        snapshot.get(OP).set("read-resource");
        snapshot.get("recursive").set(true);
        snapshot.get(OP_ADDR).set(address);
        executeOperation(snapshot);
    }
}

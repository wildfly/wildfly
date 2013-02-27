/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.manualmode.layered;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic layered distribution integration test.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
public class LayeredDistributionTestCase {

    private static final Logger log = Logger.getLogger(LayeredDistributionTestCase.class);
    private static final String CONTAINER = "jbossas-layered";
    private static final String AS_PATH = ".." + File.separator + CONTAINER;
    private final File layersDir = new File(AS_PATH + File.separator + "modules" + File.separator + "system" + File.separator + "layers");
    private static final String TEST_LAYER = "test";
    private static final String PRODUCT_NAME = "Test-Product";
    private static final String PRODUCT_VERSION = "1.0.0-Test";
    private static final String DEPLOYMENT = "test-deployment";
    private static final String webURI = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;    

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-deployment.war");
        // set dependency to the test deployment
        war.addClass(LayeredTestServlet.class);
        war.setManifest(new StringAsset(
                "Manifest-Version: 1.0" + System.getProperty("line.separator")
                + "Dependencies: org.jboss.ldtc, org.jboss.modules" + System.getProperty("line.separator")));
        return war;
    }

    @Test
    @InSequence(-1)
    public void before() throws Exception {

        buildLayer(TEST_LAYER);
        buildProductModule(TEST_LAYER);
        buildTestModule(TEST_LAYER);


        log.info("===starting server===");
        controller.start(CONTAINER);
        log.info("===appserver started===");
        //deployer.deploy(DEPLOYMENT);
        //log.info("===deployment deployed===");
    }

    @Test
    @InSequence(1)
    public void after() throws Exception {
        try {
            //deployer.undeploy(DEPLOYMENT);
            //log.info("===deployment undeployed===");
        } finally {
            controller.stop(CONTAINER);
            log.info("===appserver stopped===");
        }
    }

    @Test
    public void testLayeredProductVersion() throws Throwable {

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();

        ModelNode readAttrOp = new ModelNode();
        readAttrOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        readAttrOp.get(ModelDescriptionConstants.NAME).set("product-name");

        ModelNode result = ManagementOperations.executeOperation(client, readAttrOp);
        Assert.assertEquals(result.asString(), PRODUCT_NAME);

        readAttrOp.get(ModelDescriptionConstants.NAME).set("product-version");
        result = ManagementOperations.executeOperation(client, readAttrOp);
        Assert.assertEquals(result.asString(), PRODUCT_VERSION);

    }

    @Test
    public void testLayeredDeployment() throws Throwable {
        
        deployer.deploy(DEPLOYMENT);
        
        // test that the deployment can access class from a layered module
        String response = HttpRequest.get(webURI + "/test-deployment/LayeredTestServlet", 10, TimeUnit.SECONDS);
        Assert.assertTrue(response.contains("LayeredTestServlet"));
        
        deployer.undeploy(DEPLOYMENT);
        
    }

    private void buildLayer(String layer) throws Exception {

        File asDir = new File(AS_PATH);
        log.info("AS dir:" + asDir);
        Assert.assertTrue(asDir.exists());

        Assert.assertTrue(layersDir.exists());

        File layerDir = new File(layersDir, layer);
        if (layerDir.exists()) {
            FileUtils.deleteDirectory(layerDir);
        }

        // set layers.conf
        File layersConf = new File(AS_PATH, "modules" + File.separator + "layers.conf");
        FileUtils.writeStringToFile(layersConf, "layers=test" + System.getProperty("line.separator"));

    }

    private void buildProductModule(String layer) throws Exception {
        File layerDir = new File(layersDir, layer);
        File moduleDir = new File(layerDir, "org" + File.separator + "jboss" + File.separator + "as" + File.separator
                + "product" + File.separator + "test");
        Assert.assertTrue(moduleDir.mkdirs());
        File moduleXmlFile = new File(moduleDir, "module.xml");
        FileUtils.copyInputStreamToFile(LayeredDistributionTestCase.class.getResourceAsStream("/layered/product-module.xml"),
                moduleXmlFile);

        File manifestDir = new File(moduleDir, "classes" + File.separator + "META-INF");
        Assert.assertTrue(manifestDir.mkdirs());

        File moduleManifestFile = new File(manifestDir, "MANIFEST.MF");
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("JBoss-Product-Release-Name", PRODUCT_NAME);
        m.getMainAttributes().putValue("JBoss-Product-Release-Version", PRODUCT_VERSION);
        OutputStream manifestStream = new BufferedOutputStream(new FileOutputStream(moduleManifestFile));
        m.write(manifestStream);
        manifestStream.flush();
        manifestStream.close();

        // set product.conf        
        File binDir = new File(AS_PATH, "bin");
        Assert.assertTrue(binDir.exists());
        File productConf = new File(binDir, "product.conf");
        if (productConf.exists()) productConf.delete();
        FileUtils.writeStringToFile(productConf, "slot=test" + System.getProperty("line.separator"));

    }

    private void buildTestModule(String layer) throws Exception {

        File layerDir = new File(layersDir, layer);
        File moduleDir = new File(layerDir, "org" + File.separator + "jboss" + File.separator + "ldtc" + File.separator
                + File.separator + "main");
        Assert.assertTrue(moduleDir.mkdirs());
        File moduleXmlFile = new File(moduleDir, "module.xml");
        FileUtils.copyInputStreamToFile(LayeredDistributionTestCase.class.getResourceAsStream("/layered/test-module.xml"),
                moduleXmlFile);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-module.jar");
        jar.addClass(LayeredTestModule.class);
        File jarFile = new File(moduleDir, "test-module.jar");
        jar.as(ZipExporter.class).exportTo(jarFile);
    }
}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.modules;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test modules with resource-roots with absolute/relative paths (MODULES-218)
 * @author Martin Simka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ModuleResourcesTestCase extends AbstractCliTestBase {
    private static final String MODULE_RESOURCE_MODULE_NAME = "module.resource.test";
    private static final String ABSOLUTE_RESOURCE_MODULE_NAME = "absolute.resource.test";
    private static final String MODULE_RESOURCE_JAR_NAME = "module-resource.jar";
    private static final String ABSOLUTE_RESOURCE_JAR_NAME = "absolute-resource.jar";
    private static File moduleResource;
    private static File absoluteResource;

    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        createResources();
        AbstractCliTestBase.initCLI();
        addModule(MODULE_RESOURCE_MODULE_NAME, true, false);
        addModule(ABSOLUTE_RESOURCE_MODULE_NAME, false, true);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        removeModule(ABSOLUTE_RESOURCE_MODULE_NAME);
        removeModule(MODULE_RESOURCE_MODULE_NAME);
        AbstractCliTestBase.closeCLI();
        deleteResources();
    }

    @Deployment(name = "resourceTest", managed = false)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "Test.war")
                .addClass(SimpleTestServlet.class)
                .addAsManifestResource(new StringAsset("Dependencies: " + MODULE_RESOURCE_MODULE_NAME
                        + "," + ABSOLUTE_RESOURCE_MODULE_NAME + "\n"), "MANIFEST.MF");
    }

    private static void addModule(String name, boolean addModuleResources, boolean addAbsoluteResources) throws IOException {
        cli.sendLine("module add --name=" + name
                + (addModuleResources ? " --resources=" + moduleResource.getCanonicalPath() : "")
                + (addAbsoluteResources ? " --absolute-resources=" + absoluteResource.getCanonicalPath() : ""));
    }

    @Test
    public void testResources() throws IOException, TimeoutException, ExecutionException {
        deployer.deploy("resourceTest");

        // test module resources
        String address = "http://" + TestSuiteEnvironment.getServerAddress()
                + ":8080/Test/SimpleTestServlet?action=" + SimpleTestServlet.ACTION_TEST_MODULE_RESOURCE;
        String response = HttpRequest.get(address, 1000, 10, TimeUnit.SECONDS);
        Assert.assertEquals(ModuleResource.MODULE_RESOURCE, response);

        // test absolute resources
        address = "http://" + TestSuiteEnvironment.getServerAddress()
                + ":8080/Test/SimpleTestServlet?action=" + SimpleTestServlet.ACTION_TEST_ABSOLUTE_RESOURCE;
        response = HttpRequest.get(address, 1000, 10, TimeUnit.SECONDS);
        Assert.assertEquals(AbsoluteResource.ABSOLUTE_RESOURCE, response);

        deployer.undeploy("resourceTest");
    }

    private static void removeModule(String name) {
        cli.sendLine("module remove --name=" + name);
    }

    private static void createResources() {
        final String tempDir = TestSuiteEnvironment.getTmpDir();

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(ModuleResource.class);
        moduleResource = new File(tempDir, MODULE_RESOURCE_JAR_NAME);
        jar.as(ZipExporter.class).exportTo(moduleResource, true);

        jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(AbsoluteResource.class);
        absoluteResource = new File(tempDir, ABSOLUTE_RESOURCE_JAR_NAME);
        jar.as(ZipExporter.class).exportTo(absoluteResource, true);
    }

    private static void deleteResources() {
        moduleResource.delete();
        absoluteResource.delete();
    }
}

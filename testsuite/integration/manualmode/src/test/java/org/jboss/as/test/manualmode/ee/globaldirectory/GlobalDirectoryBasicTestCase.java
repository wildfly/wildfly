/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.as.test.manualmode.ee.globaldirectory;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PATH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic test that ensures that a jar file and a property file added into a global-directory are available in a deployment.
 *
 * @author Yeray Borges
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GlobalDirectoryBasicTestCase {
    private static final PathAddress GLOBAL_DIRECTORY_TEST_APP_LIBS = PathAddress.pathAddress(SUBSYSTEM, "ee").append("global-directory", "test-app-libs");
    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String DEPLOYMENT_WAR = "deployment-war";
    private static final String DEPLOYMENT_EAR = "deployment-ear";

    @ArquillianResource
    private static ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    private ManagementClient managementClient;
    private Path globalDirectoryPath;

    public void setupServer(ManagementClient managementClient) throws Exception {
        final ModelNode op = Operations.createAddOperation(GLOBAL_DIRECTORY_TEST_APP_LIBS.toModelNode());
        op.get(PATH).set(globalDirectoryPath.toString());
        ModelTestUtils.checkOutcome(managementClient.getControllerClient().execute(op));
    }

    public void tearDown(ManagementClient managementClient) throws Exception {
        final ModelNode op = Operations.createRemoveOperation(GLOBAL_DIRECTORY_TEST_APP_LIBS.toModelNode());
        ModelTestUtils.checkOutcome(managementClient.getControllerClient().execute(op));
    }

    @Deployment(name = DEPLOYMENT_WAR, testable = false, managed = false)
    public static Archive<?> deployWr() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "web-app.war");
        war.addClasses(EchoServlet.class);

        return war;
    }

    @Deployment(name = DEPLOYMENT_EAR, testable = false, managed = false)
    public static Archive<?> deployEar() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ear-app.ear");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "web-app-in-ear.war");
        war.addClasses(EchoServlet.class);
        ear.addAsModule(war);

        return ear;
    }

    @Before
    public void init() throws Exception {
        prepareGlobalDirectory();
        controller.start(DEFAULT_JBOSSAS);

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");
        setupServer(managementClient);

        deployer.deploy(DEPLOYMENT_WAR);
        deployer.deploy(DEPLOYMENT_EAR);
    }

    @After
    public void tearDown() throws Exception {
        deployer.undeploy(DEPLOYMENT_WAR);
        deployer.undeploy(DEPLOYMENT_EAR);

        tearDown(managementClient);
        controller.stop(DEFAULT_JBOSSAS);
        FileUtils.deleteDirectory(globalDirectoryPath.toFile());
    }

    @Test
    public void testApplications() throws IOException, TimeoutException, ExecutionException {
        String address = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/web-app/echoServlet";
        String out = HttpRequest.get(address, 60, TimeUnit.SECONDS);
        Assert.assertTrue("Unexpected message from echoServlet in war file", out.contains("echo-library-1 Message from the servlet Key=test, Value=test-value Key=sub-test, Value=sub-test-value"));

        address = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/web-app-in-ear/echoServlet";
        out = HttpRequest.get(address, 60, TimeUnit.SECONDS);
        Assert.assertTrue("Unexpected message from echoServlet in war file", out.contains("echo-library-1 Message from the servlet Key=test, Value=test-value Key=sub-test, Value=sub-test-value"));
    }

    private void prepareGlobalDirectory() throws IOException {
        final String jbossHome = System.getProperty("jboss.home", null);
        if (jbossHome == null) {
            throw new IllegalStateException("-Djboss.home not set");
        }
        globalDirectoryPath = Paths.get(jbossHome, "standalone", "global-directory").toAbsolutePath();
        Files.createDirectories(globalDirectoryPath);
        prepareLibs();
        prepareProperties();
    }

    private void prepareLibs() {
        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(EchoUtility.class);
        lib.as(ZipExporter.class).exportTo(globalDirectoryPath.resolve("lib.jar").toFile());
    }

    private void prepareProperties() throws IOException {
        Files.createDirectories(globalDirectoryPath.resolve("sub"));
        Path testResourcesPath = Paths.get("src","test", "resources");

        Path source = testResourcesPath.resolve(GlobalDirectoryBasicTestCase.class.getPackage().getName().replace(".", File.separator))
                .resolve("sub")
                .resolve("sub-global-directory.properties");
        Path target = globalDirectoryPath
                .resolve("sub")
                .resolve("sub-global-directory.properties");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        source = testResourcesPath.resolve(GlobalDirectoryBasicTestCase.class.getPackage().getName().replace(".", File.separator))
                .resolve("global-directory.properties");
        target = globalDirectoryPath
                .resolve("global-directory.properties");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}

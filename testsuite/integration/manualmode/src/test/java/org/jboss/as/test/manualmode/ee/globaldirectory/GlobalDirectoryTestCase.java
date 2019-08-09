/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.ee.globaldirectory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.PropertyPermission;

import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.manualmode.ee.globaldirectory.deployments.GlobalDirectoryDeployment;
import org.jboss.as.test.manualmode.ee.globaldirectory.deployments.GlobalDirectoryDeployment2;
import org.jboss.as.test.manualmode.ee.globaldirectory.deployments.GlobalDirectoryDeployment3;
import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibrary;
import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibraryImpl;
import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibraryImpl2;
import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibraryImpl3;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for global directory
 *
 * @author Vratislav Marek (vmarek@redhat.com)
 * @author Tomas Terem (tterem@redhat.com)
 **/
@RunWith(Arquillian.class)
@RunAsClient
public class GlobalDirectoryTestCase extends GlobalDirectoryBase {

    @Before
    public void setup() throws Exception {
        initCLI(true);
    }

    @After
    public void clean() throws IOException, InterruptedException {
        removeGlobalDirectory(GLOBAL_DIRECTORY_NAME);
        verifyDoesNotExist(GLOBAL_DIRECTORY_NAME);

        reloadServer();

        FileUtils.deleteDirectory(GLOBAL_DIRECTORY_PATH.toAbsolutePath().toFile());
        FileUtils.deleteDirectory(SECOND_GLOBAL_DIRECTORY_PATH.toAbsolutePath().toFile());
        FileUtils.deleteDirectory(TEMP_DIR);
    }

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(GlobalDirectoryDeployment.class);
        war.addAsWebInfResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?><web-app><servlet-mapping>\n" +
                "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/*</url-pattern>\n" +
                "    </servlet-mapping></web-app>"), "web.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT2, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createDeployment2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT2 + ".war");
        war.addClass(GlobalDirectoryDeployment2.class);
        war.addAsWebInfResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?><web-app><servlet-mapping>\n" +
                "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/*</url-pattern>\n" +
                "    </servlet-mapping></web-app>"), "web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT3, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createDeployment3() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT3 + ".war");
        war.addClass(GlobalDirectoryDeployment3.class);
        war.addAsWebInfResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?><web-app><servlet-mapping>\n" +
                "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/*</url-pattern>\n" +
                "    </servlet-mapping></web-app>"), "web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getProtectionDomain"), new PropertyPermission("user.dir", "read")), "permissions.xml");
        return war;
    }

    /**
     * Test checking if global directory exist
     */
    @Test
    public void testSmoke() throws IOException, InterruptedException {
        registerGlobalDirectory(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString(), true);
        verifyProperlyRegistered(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString());
    }

    /**
     * Test checking rule for only one global directory
     */
    @Test
    public void testSmokeOnlyOne() throws IOException, InterruptedException {
        registerGlobalDirectory(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString(), true);
        verifyProperlyRegistered(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString());
        reloadServer();

        final ModelNode response = registerGlobalDirectory(SECOND_GLOBAL_DIRECTORY_NAME, SECOND_GLOBAL_DIRECTORY_PATH.toString(), false);
        ModelNode outcome = response.get(OUTCOME);
        assertThat("Registration of global directory failure!", outcome.asString(), is(FAILED));
        final ModelNode failureDescription = response.get(FAILURE_DESCRIPTION);
        assertThat("Error message doesn't contains information about duplicate global directory",
                failureDescription.asString(), containsString(DUPLICATE_ERROR_GLOBAL_DIRECTORY_CODE));

        verifyDoesNotExist(SECOND_GLOBAL_DIRECTORY_NAME);
    }

    /**
     * Test for basic functionality of global directory
     * 1. Create libraries and copy them to global directory
     * 2. Define global-directory by CLI command
     * 3. Check if global-directory is registered properly and verify its attributes
     * 4. Reload server
     * 5. Deploy test application deployment
     * 6. Call some method from global-directory in deployment and verify method output
     */
    @Test
    public void testBasic() throws IOException, InterruptedException {
        createLibrary(GlobalDirectoryLibrary.class.getSimpleName(), GlobalDirectoryLibrary.class);
        createLibrary(GlobalDirectoryLibraryImpl.class.getSimpleName(), GlobalDirectoryLibraryImpl.class);

        copyLibraryToGlobalDirectory(GlobalDirectoryLibrary.class.getSimpleName());
        copyLibraryToGlobalDirectory(GlobalDirectoryLibraryImpl.class.getSimpleName());

        registerGlobalDirectory(GLOBAL_DIRECTORY_NAME);
        verifyProperlyRegistered(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString());
        reloadServer();

        try {
            deployer.deploy(DEPLOYMENT);

            Response response = client.target("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/" + DEPLOYMENT + "/global-directory/get").request().get();
            String result = response.readEntity(String.class);
            Assert.assertEquals("HELLO WORLD", result);
        } finally {
            deployer.undeploy(DEPLOYMENT);
        }
    }

    /**
     * Test reaction to corrupted jar
     * 1. Create corrupted jar and copy it to global directory
     * 2. Define global-directory by CLI command
     * 3. Check if global-directory is registered properly and verify its attributes
     * 4. Reload server
     * 5. Deploy test application deployment and verify that Exception is thrown
     * 6. Check that server log contain information about corrupted jar
     */
    @Test
    public void testCorruptedJar() throws IOException, InterruptedException {
        createCorruptedLibrary("corrupted", Arrays.asList("hello world"));
        copyLibraryToGlobalDirectory("corrupted");
        registerGlobalDirectory(GLOBAL_DIRECTORY_NAME);
        verifyProperlyRegistered(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString());
        reloadServer();

        try {
            deployer.deploy(DEPLOYMENT);
            fail("Exception should have been thrown.");
        } catch (Exception e) {
            Assert.assertEquals(DeploymentException.class, e.getClass());
        }
        logContains("WFLYSRV0276: There is an error in opening zip file " + new File(GLOBAL_DIRECTORY_FILE, "corrupted.jar").getAbsolutePath());
    }

    /**
     * Test loading order of libraries in global directory
     * 1. Create multiple jars and copy them to global directory into various folders
     * 2. Define global-directory by CLI command
     * 3. Check if global-directory is registered properly and verify its attributes
     * 4. Reload server
     * 5. Set logging level to DEBUG
     * 6. Deploy test application deployment
     * 7. Call some method from global-directory in deployment
     * 8. Verify that class from jar which is first in alphabetical order was used
     * 9. Check if server log file contains all jars in alphabetical order
     */
    @Test
    public void testLoadOrder() throws Exception {
        createLibrary(GlobalDirectoryLibrary.class.getSimpleName(), GlobalDirectoryLibrary.class);
        createLibrary(GlobalDirectoryLibraryImpl.class.getSimpleName(), GlobalDirectoryLibraryImpl.class);
        createLibrary(GlobalDirectoryLibraryImpl2.class.getSimpleName(), GlobalDirectoryLibraryImpl2.class);
        createLibrary(GlobalDirectoryLibraryImpl3.class.getSimpleName(), GlobalDirectoryLibraryImpl3.class);

        GLOBAL_DIRECTORY_PATH.toFile().mkdirs();

        File subDirectoryA = new File(GLOBAL_DIRECTORY_PATH.toFile(), "A");
        File subDirectoryAB = new File(GLOBAL_DIRECTORY_PATH.toFile(), "AB");
        File subDirectoryC = new File(GLOBAL_DIRECTORY_PATH.toFile(), "C");
        File subDirectoryC_D = new File(subDirectoryC, "D");
        File subDirectoryC_E = new File(subDirectoryC, "E");
        File subDirectoryC_D_F = new File(subDirectoryC_D, "F");

        copyLibraryToGlobalDirectory(GlobalDirectoryLibrary.class.getSimpleName());
        copyLibraryToDirectory(GlobalDirectoryLibrary.class.getSimpleName(), subDirectoryA.toString());
        copyLibraryToDirectory(GlobalDirectoryLibraryImpl.class.getSimpleName(), subDirectoryA.toString());
        copyLibraryToDirectory(GlobalDirectoryLibraryImpl.class.getSimpleName(), subDirectoryAB.toString());
        copyLibraryToDirectory(GlobalDirectoryLibraryImpl3.class.getSimpleName(), subDirectoryC_D_F.toString());
        copyLibraryToDirectory(GlobalDirectoryLibraryImpl2.class.getSimpleName(), subDirectoryC_E.toString());
        copyLibraryToDirectory(GlobalDirectoryLibraryImpl3.class.getSimpleName(), subDirectoryC_E.toString());

        registerGlobalDirectory(GLOBAL_DIRECTORY_NAME);
        verifyProperlyRegistered(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString());
        reloadServer();

        initCLI(true);
        cli.sendLine("/subsystem=logging/logger=org.jboss.as.server.moduleservice:add(level=DEBUG)");

        try {
            deployer.deploy(DEPLOYMENT3);

            Response response = client.target("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/" + DEPLOYMENT3 + "/global-directory/get").request().get();
            String result = response.readEntity(String.class);

            Assert.assertThat("C/D/F/GlobalDirectoryLibraryImpl3.jar should be used instead of C/E/GlobalDirectoryLibraryImpl3.jar.",
                    result, not(containsString(new File(subDirectoryC_E, "GlobalDirectoryLibraryImpl3.jar").toString())));
            Assert.assertThat(result, containsString(new File(subDirectoryC_D_F, "GlobalDirectoryLibraryImpl3.jar").toString()));

            checkJarLoadingOrder(new String[]{
                    "Added " + GLOBAL_DIRECTORY_FILE.getAbsolutePath() + " directory as resource root",
                    "Added " + new File(GLOBAL_DIRECTORY_FILE, "GlobalDirectoryLibrary.jar").getAbsolutePath() + " jar file",
                    "Added " + new File(subDirectoryA, "GlobalDirectoryLibrary.jar").getAbsolutePath() + " jar file",
                    "Added " + new File(subDirectoryA, "GlobalDirectoryLibraryImpl.jar").getAbsolutePath() + " jar file",
                    "Added " + new File(subDirectoryAB, "GlobalDirectoryLibraryImpl.jar").getAbsolutePath() + " jar file",
                    "Added " + new File(subDirectoryC_D_F, "GlobalDirectoryLibraryImpl3.jar").getAbsolutePath() + " jar file",
                    "Added " + new File(subDirectoryC_E, "GlobalDirectoryLibraryImpl2.jar").getAbsolutePath() + " jar file",
                    "Added " + new File(subDirectoryC_E, "GlobalDirectoryLibraryImpl3.jar").getAbsolutePath() + " jar file"
            });
        } finally {
            cli.sendLine("/subsystem=logging/logger=org.jboss.as.server.moduleservice:remove()");
            deployer.undeploy(DEPLOYMENT3);
        }
    }

    /**
     * Test for loading property files
     * 1. Create text file and copy it to global directory
     * 2. Define global-directory by CLI command
     * 3. Check if global-directory are registered properly and verify its attributes
     * 4. Reload server
     * 5. Deploy test application deployment
     * 6. Call some method (such that its result depend on content of the property file) from global-directory in deployment
     * 7. Verify that result contains an expected output
     */
    @Test
    public void testPropertyFile() throws IOException, InterruptedException {
        createLibrary(GlobalDirectoryLibrary.class.getSimpleName(), GlobalDirectoryLibrary.class);
        createLibrary(GlobalDirectoryLibraryImpl2.class.getSimpleName(), GlobalDirectoryLibraryImpl2.class);

        String propertyFileName = "properties";
        String propertyFileString = "PROPERTY FILE";

        createTextFile(propertyFileName, Arrays.asList(propertyFileString));

        copyLibraryToGlobalDirectory(GlobalDirectoryLibrary.class.getSimpleName());
        copyLibraryToGlobalDirectory(GlobalDirectoryLibraryImpl2.class.getSimpleName());
        copyTextFileToGlobalDirectory(propertyFileName);

        registerGlobalDirectory(GLOBAL_DIRECTORY_NAME);
        verifyProperlyRegistered(GLOBAL_DIRECTORY_NAME, GLOBAL_DIRECTORY_PATH.toString());
        reloadServer();

        try {
            deployer.deploy(DEPLOYMENT2);

            Response response = client.target("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/" + DEPLOYMENT2 + "/global-directory/get").request().get();
            String result = response.readEntity(String.class);
            Assert.assertEquals(propertyFileString, result);
        } finally {
            deployer.undeploy(DEPLOYMENT2);
        }
    }
}

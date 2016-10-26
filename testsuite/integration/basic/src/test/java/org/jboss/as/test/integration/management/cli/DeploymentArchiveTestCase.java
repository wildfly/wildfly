/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.cli;

import org.jboss.as.test.integration.management.base.AbstractCliTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This tests a CLI deployment archive functionality.
 * See https://community.jboss.org/wiki/CLIDeploymentArchive
 *
 * @author Ivo Studensky <istudens@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentArchiveTestCase extends AbstractCliTestBase {

    private static final String MODULE_NAME = "org.jboss.test.deploymentarchive";

    private static final String WEB_ARCHIVE_NAME = "deploymentarchive";
    private static final String MODULE_ARCHIVE = "deploymentarchivemodule.jar";
    private static final String MODULE_XML_FILE = "module.xml";

    private static final String DEPLOY_SCR =
            "deploy " + WEB_ARCHIVE_NAME + ".war\n" +
            "module add --name=" + MODULE_NAME +
            " --resources=" + MODULE_ARCHIVE +
            " --module-xml=" + MODULE_XML_FILE;
    private static final String UNDEPLOY_SCR =
            "undeploy " + WEB_ARCHIVE_NAME + ".war\n" +
            "module remove --name=" + MODULE_NAME;

    private static final String MODULE_XML =
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
              "<module xmlns=\"urn:jboss:module:1.5\" name=\"" + MODULE_NAME + "\" slot=\"main\">" +
              "    <resources>" +
              "        <resource-root path=\"" + MODULE_ARCHIVE + "\"/>" +
              "    </resources>" +
              "</module>";


    private static File cliFile;

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar")
                .addClass(DeploymentArchiveTestCase.class);
    }

    @BeforeClass
    public static void before() throws Exception {
        cliFile = createCliArchive();
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
        cliFile.delete();
    }

    @Test
    public void testDeployUndeploy() throws Exception {
        testDeploy();
        testUndeploy();
    }

    private void testDeploy() throws Exception {

        // check whether the module is not deployed
        final File testModuleRoot = new File(getModulePath(), MODULE_NAME.replace('.', File.separatorChar));
        assertFalse("Module is already deployed at " + testModuleRoot, testModuleRoot.exists());

        // deploy to server
        cli.sendLine("deploy " + cliFile.getAbsolutePath());

        // check war deployment
        String response = HttpRequest.get(getBaseURL(url) + WEB_ARCHIVE_NAME + "/SimpleServlet", 1000, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);

        // check module deployment
        assertTrue("Module deployment failed! Module dir does not exist: " + testModuleRoot, testModuleRoot.exists());
    }

    private void testUndeploy() throws Exception {

        //undeploy
        cli.sendLine("undeploy --path=" + cliFile.getAbsolutePath());

        // check undeployment
        assertTrue(checkUndeployed(getBaseURL(url) + WEB_ARCHIVE_NAME + "/SimpleServlet"));

        // check module undeployment
        final File testModuleRoot = new File(getModulePath(), MODULE_NAME.replace('.', File.separatorChar));
        assertFalse("Module undeployment failed.", testModuleRoot.exists());
    }

    private static File createCliArchive() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, WEB_ARCHIVE_NAME + ".war");
        webArchive.addClass(SimpleServlet.class);

        final JavaArchive moduleArchive = ShrinkWrap.create(JavaArchive.class, MODULE_ARCHIVE);
        moduleArchive.addClass(DeploymentArchiveTestCase.class);

        final GenericArchive cliArchive = ShrinkWrap.create(GenericArchive.class, "deploymentarchive.cli");
        cliArchive.add(new StringAsset(DEPLOY_SCR), "deploy.scr");
        cliArchive.add(new StringAsset(UNDEPLOY_SCR), "undeploy.scr");
        cliArchive.add(webArchive, "/", ZipExporter.class);
        cliArchive.add(moduleArchive, "/", ZipExporter.class);
        cliArchive.add(new StringAsset(MODULE_XML), "/", "module.xml");

        final String tempDir = TestSuiteEnvironment.getTmpDir();
        final File file = new File(tempDir, "deploymentarchive.cli");
        cliArchive.as(ZipExporter.class).exportTo(file, true);
        return file;
    }

    private File getModulePath() {
        String modulePath = TestSuiteEnvironment.getSystemProperty("module.path", null);
        if (modulePath == null) {
            String jbossHome = TestSuiteEnvironment.getSystemProperty("jboss.home", null);
            if (jbossHome == null) {
                throw new IllegalStateException(
                        "Neither -Dmodule.path nor -Djboss.home were set");
            }
            modulePath = jbossHome + File.separatorChar + "modules";
        } else {
            modulePath = modulePath.split(File.pathSeparator)[0];
        }
        File moduleDir = new File(modulePath);
        if (!moduleDir.exists()) {
            throw new IllegalStateException(
                    "Determined module path does not exist");
        }
        if (!moduleDir.isDirectory()) {
            throw new IllegalStateException(
                    "Determined module path is not a dir");
        }
        return moduleDir;
    }

}

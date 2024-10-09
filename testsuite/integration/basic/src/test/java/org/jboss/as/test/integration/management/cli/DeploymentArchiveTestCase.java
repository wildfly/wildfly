/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
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
            "deploy " + WEB_ARCHIVE_NAME + ".war";
    private static final String MODULE_ADD_SCR =
            "module add --name=" + MODULE_NAME +
            " --resources=" + MODULE_ARCHIVE +
            " --module-xml=" + MODULE_XML_FILE;
    private static final String UNDEPLOY_SCR =
            "undeploy " + WEB_ARCHIVE_NAME + ".war";
    private static final String MODULE_REMOVE_SCR =
            "module remove --name=" + MODULE_NAME;

    private static final String MODULE_XML =
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
              "<module xmlns=\"urn:jboss:module:1.9\" name=\"" + MODULE_NAME + ">" +
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
        cliFile = createCliArchive(!AssumeTestGroupUtil.isBootableJar());
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

        File testModuleRoot = null;
        // check whether the module is not deployed
        if (!AssumeTestGroupUtil.isBootableJar()) {
            // "module add/remove" operations doesn't make sense in bootablejar
            // CLI isn't included in bootablejar and "module" is local command. It operates with module on its own
            // MODULEPATH. It could be used with bootable jar with workaround:
            //   $ JBOSS_HOME=<bootablejar_install_dir> jboss.cli.sh -c
            // install-dir is by default folder with random suffix in /tmp
            testModuleRoot = new File(TestModule.getModulesDirectory(false), MODULE_NAME.replace('.', File.separatorChar));
            assertFalse("Module is already deployed at " + testModuleRoot, testModuleRoot.exists());
        }

        // deploy to server
        cli.sendLine("deploy " + cliFile.getAbsolutePath());

        // check war deployment
        String response = HttpRequest.get(getBaseURL(url) + WEB_ARCHIVE_NAME + "/SimpleServlet", 1000, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);

        // check module deployment
        if (!AssumeTestGroupUtil.isBootableJar()) {
            assertTrue("Module deployment failed! Module dir does not exist: " + testModuleRoot, testModuleRoot.exists());
        }
    }

    private void testUndeploy() throws Exception {

        //undeploy
        cli.sendLine("undeploy --path=" + cliFile.getAbsolutePath());

        // check undeployment
        assertTrue(checkUndeployed(getBaseURL(url) + WEB_ARCHIVE_NAME + "/SimpleServlet"));

        // check module undeployment
        if (!AssumeTestGroupUtil.isBootableJar()) {
            final File testModuleRoot = new File(TestModule.getModulesDirectory(false), MODULE_NAME.replace('.', File.separatorChar));
            assertFalse("Module undeployment failed.", testModuleRoot.exists());
        }
    }

    private static File createCliArchive(boolean includeModule) {
        String deployScript = DEPLOY_SCR;
        String undeployScript = UNDEPLOY_SCR;

        if (includeModule) {
            String modulesPath = TestModule.getModulesDirectory(true).getAbsolutePath();
            String moduleRootDirArgument = " --module-root-dir=" + modulesPath;
            deployScript = deployScript + "\n" + MODULE_ADD_SCR + moduleRootDirArgument;
            undeployScript = undeployScript + "\n" + MODULE_REMOVE_SCR + moduleRootDirArgument;
        }

        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, WEB_ARCHIVE_NAME + ".war");
        webArchive.addClass(SimpleServlet.class);
        final GenericArchive cliArchive = ShrinkWrap.create(GenericArchive.class, "deploymentarchive.cli");
        cliArchive.add(new StringAsset(deployScript), "deploy.scr");
        cliArchive.add(new StringAsset(undeployScript), "undeploy.scr");
        cliArchive.add(webArchive, "/", ZipExporter.class);

        cliArchive.add(new StringAsset(MODULE_XML), "/", "module.xml");

        if (includeModule) {
            final JavaArchive moduleArchive = ShrinkWrap.create(JavaArchive.class, MODULE_ARCHIVE);
            moduleArchive.addClass(DeploymentArchiveTestCase.class);
            cliArchive.add(moduleArchive, "/", ZipExporter.class);
        }

        final String tempDir = TestSuiteEnvironment.getTmpDir();
        final File file = new File(tempDir, "deploymentarchive.cli");
        cliArchive.as(ZipExporter.class).exportTo(file, true);
        return file;
    }

}

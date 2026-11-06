/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli;

import org.jboss.as.test.integration.management.base.AbstractCliTestBase;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class DeploymentScannerTestCase extends AbstractCliTestBase {

    private static final String tempDir = TestSuiteEnvironment.getTmpDir();
    private static File warFile;
    private static File deployDir;

    @ArquillianResource URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeploymentScannerTestCase.class);
        return ja;
    }

    @BeforeAll
    public static void before() throws Exception {

        deployDir = new File(tempDir + File.separator + "tempDeployment");
        if (deployDir.exists()) {
            FileUtils.deleteDirectory(deployDir);
        }
        assertTrue(deployDir.mkdir(), "Unable to create deployment scanner directory.");
        AbstractCliTestBase.initCLI();
    }

    @AfterAll
    public static void after() throws Exception {
        FileUtils.deleteDirectory(deployDir);
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testAddRemoveDeploymentScanner() throws Exception {
        addDeploymentScanner();
        removeDeploymentScanner();
    }

    private void addDeploymentScanner() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        warFile = new File(deployDir.getAbsolutePath() + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        // add deployment scanner
        String path =  deployDir.getAbsolutePath();
        path = path.replaceAll("\\\\", "/");
        cli.sendLine("/subsystem=deployment-scanner/scanner=testScanner:add(scan-interval=1000,path=\"" + path +"\")");

        // wait for deployment
        Thread.sleep(2000);

        // check that the app has been deployed
        File marker = new File(deployDir.getAbsolutePath() + File.separator + "SimpleServlet.war.deployed");
        assertTrue(marker.exists());

        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
        assertTrue(response.indexOf("SimpleServlet") >=0, "Invalid response: " + response);

    }

    private void removeDeploymentScanner() throws Exception {

        // remove deployment scanner
        cli.sendLine("/subsystem=deployment-scanner/scanner=testScanner:remove()");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());

        // delete deployment
        assertTrue(warFile.delete(), "Could not delete deployed file.");

        // wait for deployment
        Thread.sleep(2000);

        // check that the deployment is still live
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
        assertTrue(response.indexOf("SimpleServlet") >=0, "Invalid response: " + response);

        // undeploy using CLI
        cli.sendLine("undeploy SimpleServlet.war");
        assertTrue(checkUndeployed(getBaseURL(url) + "SimpleServlet/SimpleServlet"));
    }
}

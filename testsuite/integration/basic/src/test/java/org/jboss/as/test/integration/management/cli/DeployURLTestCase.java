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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class DeployURLTestCase extends AbstractCliTestBase {

    private static WebArchive war;
    private static File warFile;

    @ArquillianResource URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeployURLTestCase.class);
        return ja;
    }

    @BeforeAll
    public static void before() throws Exception {
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version1"), "page.html");
        String tempDir = TestSuiteEnvironment.getTmpDir();
        warFile = new File(tempDir + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        AbstractCliTestBase.initCLI();
    }

    @AfterAll
    public static void after() throws Exception {
        warFile.delete();
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testDeployRedeployUndeploy() throws Exception {
        testDeploy();
        testRedeploy();
        testUndeploy();
    }

    public void testDeploy() throws Exception {

        // deploy to server
        cli.sendLine("deploy --url=" + warFile.toURI().toURL().toExternalForm() + " --name=" + warFile.getName());

        // check deployment
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 1000, 10, TimeUnit.SECONDS);
        assertTrue(response.indexOf("SimpleServlet") >=0, "Invalid response: " + response);
    }

    public void testRedeploy() throws Exception {

        // check we have original deployment
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/page.html", 10, TimeUnit.SECONDS);
        assertTrue(response.indexOf("Version1") >=0, "Invalid response: " + response);

        // update the deployment - replace page.html
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version2"), "page.html");
        new ZipExporterImpl(war).exportTo(warFile, true);


        // redeploy to server
        Assertions.assertFalse(cli.sendLine("deploy --url=" + warFile.toURI().toURL().toExternalForm() + " --name=" + warFile.getName(), true));

        // force redeploy
        cli.sendLine("deploy --url=" + warFile.toURI().toURL().toExternalForm() + " --name=" + warFile.getName() + " --force");

        // check that new version is running
        final long firstTry = System.currentTimeMillis();
        response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/page.html", 1000, 10, TimeUnit.SECONDS);
        while(response.indexOf("Version2") < 0) {
            if(System.currentTimeMillis() - firstTry >= 1000) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                break;
            } finally {
                response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/page.html", 1000, 10, TimeUnit.SECONDS);
            }
        }
        assertTrue(response.indexOf("Version2") >=0, "Invalid response: " + response);
    }

    public void testUndeploy() throws Exception {

        //undeploy
        cli.sendLine("undeploy " + warFile.getName());

        // check undeployment
        assertTrue(checkUndeployed(getBaseURL(url) + "SimpleServlet/SimpleServlet"));
    }
}

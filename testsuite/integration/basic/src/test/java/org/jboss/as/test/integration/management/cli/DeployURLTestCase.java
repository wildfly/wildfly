/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli;

import org.jboss.as.test.integration.management.base.AbstractCliTestBase;

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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
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

    @BeforeClass
    public static void before() throws Exception {
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version1"), "page.html");
        String tempDir = TestSuiteEnvironment.getTmpDir();
        warFile = new File(tempDir + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        AbstractCliTestBase.initCLI();
    }

    @AfterClass
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
        assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);
    }

    public void testRedeploy() throws Exception {

        // check we have original deployment
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/page.html", 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("Version1") >=0);

        // update the deployment - replace page.html
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version2"), "page.html");
        new ZipExporterImpl(war).exportTo(warFile, true);


        // redeploy to server
        Assert.assertFalse(cli.sendLine("deploy --url=" + warFile.toURI().toURL().toExternalForm() + " --name=" + warFile.getName(), true));

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
        assertTrue("Invalid response: " + response, response.indexOf("Version2") >=0);
    }

    public void testUndeploy() throws Exception {

        //undeploy
        cli.sendLine("undeploy " + warFile.getName());

        // check undeployment
        assertTrue(checkUndeployed(getBaseURL(url) + "SimpleServlet/SimpleServlet"));
    }
}

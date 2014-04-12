/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
public class DeployTestCase extends AbstractCliTestBase {

    private static WebArchive war;
    private static File warFile;

    @ArquillianResource URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeployTestCase.class);
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

    @Test
    public void testContentObjectDeploy() throws Exception {
        testWFLY3184();
        testUndeploy();
    }

    public void testDeploy() throws Exception {

        // deploy to server
        cli.sendLine("deploy " + warFile.getAbsolutePath());

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
        Assert.assertFalse(cli.sendLine("deploy " + warFile.getAbsolutePath(), true));

        // force redeploy
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --force");

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
        cli.sendLine("undeploy SimpleServlet.war");

        // check undeployment
        assertTrue(checkUndeployed(getBaseURL(url) + "SimpleServlet/SimpleServlet"));
    }

    private void testWFLY3184() throws Exception {

        // deploy to server
        cli.sendLine("/deployment="+warFile.getName() +":add(enabled=true,content={url=" + warFile.toURI().toURL().toExternalForm() + "})");

        // check deployment
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 1000, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.contains("SimpleServlet"));

    }
}

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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
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
        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        AbstractCliTestBase.before();
    }
    
    @AfterClass
    public static void after() throws Exception {
        warFile.delete();
        AbstractCliTestBase.after();
    }
    
    @Test
    public void testDeployRedeployUndeploy() throws Exception {
        testDeploy();
        testRedeploy();
        testUndeploy();
    }
    
    public void testDeploy() throws Exception {
                
        // deploy to server        
        cli.sendLine("deploy " + warFile.getAbsolutePath(), true);
        String line = cli.readLine(1000);
        assertTrue("Deployment failed: " + line, line.indexOf("deployed successfully") >= 0);        
        
        // check deployment
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
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
        cli.sendLine("deploy " + warFile.getAbsolutePath(), true);
        String line = cli.readLine(1000);
        // check that this fails
        assertFalse("Deployment failed: " + line, line.indexOf("deployed successfully") >= 0);        
        
        // force redeploy
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --force", true);
        line = cli.readLine(1000);
        // check that now it is ok
        assertTrue("Deployment failed: " + line, line.indexOf("deployed successfully") >= 0);       
        
        // check that new version is running
        response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/page.html", 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("Version2") >=0);
    }
    
    public void testUndeploy() throws Exception {
        
        //undeploy
        cli.sendLine("undeploy SimpleServlet.war", true);
        String line = cli.readLine(1000);
        assertTrue("Undeployment failed:" + line, line.indexOf("Successfully undeployed") >= 0);        
        
        // check undeployment
        boolean getFailed = false;
        String response = null;
        try {
            response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            getFailed = true;
        }
        assertTrue("Deployment still exists:" + response, getFailed);
    }
    
    
}

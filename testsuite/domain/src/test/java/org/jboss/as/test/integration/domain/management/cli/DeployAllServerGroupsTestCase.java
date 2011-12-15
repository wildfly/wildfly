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
package org.jboss.as.test.integration.domain.management.cli;

import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import static org.junit.Assert.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class DeployAllServerGroupsTestCase extends AbstractCliTestBase {
    
    private static WebArchive war;
    private static File warFile;

    
    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeployAllServerGroupsTestCase.class);
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
                
        // deploy to all servers
        cli.sendLine("deploy --all-server-groups " + warFile.getAbsolutePath(), true);
        
        // check that the deployment is available on all servers
        checkURL("SimpleServlet/SimpleServlet", "SimpleServlet");
    }

    public void testRedeploy() throws Exception {

        // check we have original deployment
        checkURL("SimpleServlet/page.html", "Version1");
        
        // update the deployment - replace page.html
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);        
        war.addAsWebResource(new StringAsset("Version2"), "page.html");
        new ZipExporterImpl(war).exportTo(warFile, true);
        
        
        // redeploy to all servers
        cli.sendLine("deploy --all-server-groups " + warFile.getAbsolutePath(), true);
        String line = cli.readLine(WAIT_TIMEOUT);
        // check that this fails
        assertFalse("Deployment failed: " + line, line.indexOf("deployed successfully") >= 0);        
        
        // force redeploy
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --force", true);
        
        // check that new version is running
        checkURL("SimpleServlet/page.html", "Version2");
    }
    
    public void testUndeploy() throws Exception {
        
        //undeploy
        cli.sendLine("undeploy --all-relevant-server-groups SimpleServlet.war", true);
        
        // check undeployment
        checkURL("SimpleServlet/SimpleServlet" , "SimpleServlet", true);
    }
    
    private void checkURL(String path, String content) throws Exception {
        checkURL(path, content, false);
    }
    private void checkURL(String path, String content, boolean shouldFail) throws Exception {
        for (String host : CLITestSuite.hostAddresses.keySet()) {
            String address = CLITestSuite.hostAddresses.get(host);
            for (String server : CLITestSuite.hostServers.get(host)) {
                Integer portOffset = CLITestSuite.portOffsets.get(server);
                                
                URL url = new URL("http", address, 8080 + portOffset, path);
                boolean failed = false;
                try {
                    String response = HttpRequest.get(url.toString(), 10, TimeUnit.SECONDS);
                    assertTrue(response.contains(content));                
                } catch (Exception e) {
                    failed = true;
                }                
                if (shouldFail) assertTrue(failed);
            }
        }
    }
    
}

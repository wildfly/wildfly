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

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test generic command features of CLI.
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GenericCommandTestCase extends AbstractCliTestBase {
    
    private static final String tempDir = System.getProperty("java.io.tmpdir");
    private static WebArchive war;
    private static File warFile;
    private static File deployDir;
    
    @ArquillianResource URL url;   
    
    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GenericCommandTestCase.class);
        return ja;
    }    
    
    @BeforeClass
    public static void before() throws Exception {
        deployDir = new File(tempDir + File.separator + "tempDeployment");
        if (deployDir.exists()) {
            FileUtils.deleteDirectory(deployDir);
        }
        Assert.assertTrue("Unable to create deployment scanner directory.", deployDir.mkdir());
        
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        FileUtils.deleteDirectory(deployDir);        
        AbstractCliTestBase.closeCLI();
    }    
    
    @Test
    public void testAddDeploymentScannerCommand() throws Exception {
        
        // prepare deployment
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        warFile = new File(deployDir.getAbsolutePath() + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);        
        
        // add generic deployment scanner command
        cli.sendLine("command add --node-type=/subsystem=deployment-scanner/scanner --command-name=deployment-scanner");
        
        // test created deployment scanner add command
        cli.sendLine("deployment-scanner add --name=testScanner --path=" + deployDir.getAbsolutePath());
        
        // wait for deployment
        Thread.sleep(WAIT_LINETIMEOUT * 2);
        
        // check that the deployment scanner was added and the app deployed
        File marker = new File(deployDir.getAbsolutePath() + File.separator + "SimpleServlet.war.deployed");
        Assert.assertTrue(marker.exists());

        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
        Assert.assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);        
    
        // test deployment scanner remove command
        cli.sendLine("deployment-scanner remove --name=testScanner");
        
        // check that the scanner was removed
        cli.sendLine("/subsystem=deployment-scanner/scanner=testScanner:read-resource");
        CLIOpResult res = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertFalse("Deployment scanner not removed.", res.isIsOutcomeSuccess());
        
        // undeploy test war
        cli.sendLine("undeploy SimpleServlet.war", true);
        Assert.assertTrue(checkUndeployed(getBaseURL(url) + "SimpleServlet/SimpleServlet"));        
        
    }
}

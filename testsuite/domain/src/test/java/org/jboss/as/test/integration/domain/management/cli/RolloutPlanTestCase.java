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

import java.io.File;
import java.net.URL;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.management.util.RolloutPlanBuilder;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class RolloutPlanTestCase extends AbstractCliTestBase {        
    
    private static WebArchive war;
    private static File warFile;            
    
    @BeforeClass
    public static void before() throws Exception {      
        war = ShrinkWrap.create(WebArchive.class, "RolloutPlanTestCase.war");
        war.addClass(RolloutPlanTestServlet.class);
        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir + File.separator + "RolloutPlanTestCase.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        AbstractCliTestBase.initCLI(DomainTestSupport.masterAddress);
    }    
    
    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }
    
    @Test
    public void testRolloutPlan() throws Exception {
        addRolloutPlan();                
        testRolloutPlanDeployment();        
        removeRolloutPlan();
    }
    
    private void addRolloutPlan() throws Exception {
        
        String[] serverGroups = CLITestSuite.serverGroups.keySet().toArray(new String[]{});
        
        // create rollout plan
        RolloutPlanBuilder planBuilder = new RolloutPlanBuilder();
        planBuilder.addGroup(serverGroups[0], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        planBuilder.addGroup(serverGroups[1], new RolloutPlanBuilder.RolloutPolicy(true, null, null));        
        String rolloutPlan = planBuilder.buildAsString();        
        cli.sendLine("rollout-plan add --name=testPlan --content=" + rolloutPlan);        
        
        // check it is listed
        cli.sendLine("cd /management-client-content=rollout-plans/rollout-plan");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertTrue(ls.contains("testPlan"));
        
    }
    
    private void removeRolloutPlan() throws Exception {
        
        // remove rollout plan
        cli.sendLine("rollout-plan remove --name=testPlan");        
        
        // check it is no more listed
        cli.sendLine("cd /management-client-content=rollout-plans");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        Assert.assertFalse(ls.contains("testPlan"));
        
    }
    
    private void testRolloutPlanDeployment() throws Exception {
        
        // deploy using prepared rollout plan
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups --headers={rolout id=testPlan}");
        cli.waitForPrompt(WAIT_TIMEOUT);
        
        // check that the apps were deployed in correct order
        // get application deployment times from servers
        long mainOneTime = Long.valueOf(checkURL("main-one", false));
        long mainThreeTime = Long.valueOf(checkURL("main-three", false));
        long otherTwoTime = Long.valueOf(checkURL("other-two", false));
        
        Assert.assertTrue(mainOneTime < otherTwoTime);
        Assert.assertTrue(mainThreeTime < otherTwoTime);

    }
    
    private String checkURL(String server, boolean shouldFail) throws Exception {
        String address = CLITestSuite.hostAddresses.get(getServerHost(server));
        Integer portOffset = CLITestSuite.portOffsets.get(server);
        
        URL url = new URL("http", address, 8080 + portOffset, "/RolloutPlanTestCase/RolloutServlet");
        boolean failed = false;
        String response = null;
        try {
            response = HttpRequest.get(url.toString(), 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failed = true;
            if (!shouldFail) throw new Exception("Http request failed.", e);
        }                
        if (shouldFail) Assert.assertTrue(failed);
        return response;
        
    }
    
    private String getServerHost(String server) {
        for(Entry<String, String[]> hostEntry : CLITestSuite.hostServers.entrySet()) {
            for (String hostServer : hostEntry.getValue()) if (hostServer.equals(server)) return hostEntry.getKey();
        }
        return null;
    }
    
}

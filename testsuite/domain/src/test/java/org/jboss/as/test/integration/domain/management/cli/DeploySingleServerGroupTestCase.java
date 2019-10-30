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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class DeploySingleServerGroupTestCase extends AbstractCliTestBase {

    private static WebArchive war;
    private static File warFile;

    private static String[] serverGroups;

    @BeforeClass
    public static void before() throws Exception {

        CLITestSuite.createSupport(DeploySingleServerGroupTestCase.class.getSimpleName());

        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version1"), "page.html");
        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        serverGroups = CLITestSuite.serverGroups.keySet().toArray(new String[CLITestSuite.serverGroups.size()]);

        AbstractCliTestBase.initCLI(DomainTestSupport.masterAddress);
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
        if (warFile != null && warFile.exists()) {
            Assert.assertTrue(warFile.delete());
        }

        CLITestSuite.stopSupport();
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

        // deploy to group servers
        cli.sendLine("deploy --server-groups=" + serverGroups[0] + " " + warFile.getAbsolutePath());

        // check that the deployment is available on all servers within the group and none outside
        checkURL("/SimpleServlet/SimpleServlet", "SimpleServlet", serverGroups[0]);
        checkURL("/SimpleServlet/SimpleServlet", "SimpleServlet", serverGroups[1], true);
    }

    public void testRedeploy() throws Exception {

        // check we have original deployment
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0]);

        // update the deployment - replace page.html
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version2"), "page.html");
        new ZipExporterImpl(war).exportTo(warFile, true);

        // redeploy to group servers
        assertFalse(cli.sendLine("deploy --server-groups=" + serverGroups[0] + " " + warFile.getAbsolutePath(), true));

        // force redeploy
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --force");

        // check that new version is running
        checkURL("/SimpleServlet/page.html", "Version2", serverGroups[0]);
    }

    public void testUndeploy() throws Exception {

        //undeploy
        cli.sendLine("undeploy --server-groups="  + serverGroups[0] + " SimpleServlet.war");

        // check undeployment
        checkURL("/SimpleServlet/SimpleServlet" , "SimpleServlet", serverGroups[0], true);
    }

    private void testWFLY3184() throws Exception {

        // deploy to server
        cli.sendLine("/deployment="+ warFile.getName() +":add(content={url=" + warFile.toURI().toURL().toExternalForm() + "})");
        cli.sendLine("/server-group=" + serverGroups[0] + "/deployment="+ warFile.getName() +":add(enabled=true)");

        // check that the deployment is available on all servers within the group and none outside
        checkURL("/SimpleServlet/SimpleServlet", "SimpleServlet", serverGroups[0]);
        checkURL("/SimpleServlet/SimpleServlet", "SimpleServlet", serverGroups[1], true);

    }

    private void checkURL(String path, String content, String serverGroup) throws Exception {
        checkURL(path, content, serverGroup, false);
    }
    private void checkURL(String path, String content, String serverGroup, boolean shouldFail) throws Exception {

        ArrayList<String> groupServers  = new ArrayList<String>();
        Collections.addAll(groupServers, CLITestSuite.serverGroups.get(serverGroup));

        for (String host : CLITestSuite.hostAddresses.keySet()) {
            String address = CLITestSuite.hostAddresses.get(host);
            for (String server : CLITestSuite.hostServers.get(host)) {
                if (! groupServers.contains(server)) continue;  // server not in the group
                if (! CLITestSuite.serverStatus.get(server)) continue; // server not started
                Integer portOffset = CLITestSuite.portOffsets.get(server);

                URL url = new URL("http", address, 8080 + portOffset, path);
                boolean failed = false;
                try {
                    String response = HttpRequest.get(url.toString(), 10, TimeUnit.SECONDS);
                    assertTrue(response.contains(content));
                } catch (Exception e) {
                    failed = true;
                    if (!shouldFail) throw new Exception("Http request failed.", e);
                }
                if (shouldFail) assertTrue(failed);
            }
        }
    }
}

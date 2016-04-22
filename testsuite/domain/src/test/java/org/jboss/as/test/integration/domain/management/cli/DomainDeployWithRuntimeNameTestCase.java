/*
 * Copyright (C) 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file
 * in the distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.integration.domain.management.cli;

import org.jboss.as.domain.controller.logging.DomainControllerLogger;
import org.jboss.as.test.integration.management.util.SimpleHelloWorldServlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class DomainDeployWithRuntimeNameTestCase extends AbstractCliTestBase {

    private File warFile;
    private static String[] serverGroups;
    public static final String RUNTIME_NAME = "SimpleServlet.war";
    public static final String OTHER_RUNTIME_NAME = "OtherSimpleServlet.war";
    private static final String APP_NAME = "simple1";
    private static final String OTHER_APP_NAME = "simple2";

    @BeforeClass
    public static void setup() throws Exception {

        CLITestSuite.createSupport(DomainDeployWithRuntimeNameTestCase.class.getSimpleName());
        List<String> groups = new ArrayList<>(CLITestSuite.serverGroups.keySet());
        Collections.sort(groups);
        serverGroups = groups.toArray(new String[groups.size()]);
        AbstractCliTestBase.initCLI(DomainTestSupport.masterAddress);
    }

   private File createWarFile(String content) throws IOException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "HelloServlet.war");
        war.addClass(SimpleHelloWorldServlet.class);
        war.addAsWebInfResource(SimpleHelloWorldServlet.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(new StringAsset(content), "page.html");
        File tempFile = new File(System.getProperty("java.io.tmpdir"), "HelloServlet.war");
        new ZipExporterImpl(war).exportTo(tempFile, true);
        return tempFile;
    }

    @AfterClass
    public static void cleanup() throws Exception {
        AbstractCliTestBase.closeCLI();
        CLITestSuite.stopSupport();
    }

    @After
    public void undeployAll() {
        assertThat(warFile.delete(), is(true));
        cli.sendLine("undeploy --all-relevant-server-groups " + APP_NAME, true);
        cli.sendLine("undeploy --all-relevant-server-groups " + OTHER_APP_NAME, true);
    }

    @Test
    public void testDeployWithSameRuntimeNameOnSameServerGroup() throws Exception {
        // deploy to group servers
        warFile = createWarFile("Version1");
        cli.sendLine(buildDeployCommand(serverGroups[0], RUNTIME_NAME, APP_NAME));
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[0], false);
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0], false);
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[1], true);
        warFile = createWarFile("Shouldn't be deployed, as runtime already exist");
        cli.sendLine(buildDeployCommand(serverGroups[0], RUNTIME_NAME, OTHER_APP_NAME), true);
        assertThat(cli.readOutput(), containsString(DomainControllerLogger.ROOT_LOGGER.runtimeNameMustBeUnique(APP_NAME, RUNTIME_NAME, serverGroups[0]).getMessage()));
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[0], false);
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0], false);
    }

    @Test
    public void testDeployWithSameRuntimeNameOnDifferentServerGroup() throws Exception {
        // deploy to group servers
        warFile = createWarFile("Version1");
        cli.sendLine(buildDeployCommand(serverGroups[0], RUNTIME_NAME, APP_NAME));
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[0], false);
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0], false);
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[1], true);
        warFile = createWarFile("Version2");
        cli.sendLine(buildDeployCommand(serverGroups[1], RUNTIME_NAME, OTHER_APP_NAME));
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[0], false);
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0], false);
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[1], false);
        checkURL("/SimpleServlet/page.html", "Version2", serverGroups[1], false);
    }

    @Test
    public void testDeployWithDifferentRuntimeNameOnDifferentServerGroup() throws Exception {
        // deploy to group servers
        warFile = createWarFile("Version1");
        cli.sendLine(buildDeployCommand(serverGroups[0], RUNTIME_NAME, APP_NAME));
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[0], false);
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0], false);
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[1], true);
        warFile = createWarFile("Version3");
        cli.sendLine(buildDeployCommand(serverGroups[1], OTHER_RUNTIME_NAME, OTHER_APP_NAME));
        checkURL("/SimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[0], false);
        checkURL("/SimpleServlet/page.html", "Version1", serverGroups[0], false);
        checkURL("/OtherSimpleServlet/hello", "SimpleHelloWorldServlet", serverGroups[1], false);
        checkURL("/OtherSimpleServlet/page.html", "Version3", serverGroups[1], false);
    }

    private String buildDeployCommand(String serverGroup, String runtimeName, String  name) {
        return "deploy --server-groups=" + serverGroup + " " + warFile.getAbsolutePath()  + " --runtime-name=" + runtimeName
                + " --name=" + name;
    }

    private void checkURL(String path, String content, String serverGroup, boolean shouldFail) throws Exception {
        List<String> groupServers = new ArrayList<String>();
        for (String server : CLITestSuite.serverGroups.get(serverGroup)) {
            groupServers.add(server);
        }

        for (String host : CLITestSuite.hostAddresses.keySet()) {
            String address = CLITestSuite.hostAddresses.get(host);
            for (String server : CLITestSuite.hostServers.get(host)) {
                if (!groupServers.contains(server)) {
                    continue;  // server not in the group
                }
                if (!CLITestSuite.serverStatus.get(server)) {
                    continue; // server not started
                }
                int port = 8080 + CLITestSuite.portOffsets.get(server);

                URL url = new URL("http", address, port, path);
                boolean failed = false;
                try {
                    String response = HttpRequest.get(url.toString(), 10, TimeUnit.SECONDS);
                    assertThat(response, containsString(content));
                } catch (Exception e) {
                    failed = true;
                    if (!shouldFail) {
                        throw new Exception("Http request failed.", e);
                    }
                }
                if (shouldFail) {
                    assertThat(url.toString(), failed, is(true));
                }
            }
        }
    }

}

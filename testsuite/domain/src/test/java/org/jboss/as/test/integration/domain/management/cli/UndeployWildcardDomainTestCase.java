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

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class UndeployWildcardDomainTestCase {

    private static File cliTestApp1War;
    private static File cliTestApp2War;
    private static File cliTestAnotherWar;
    private static File cliTestAppEar;

    private static String sgOne;
    private static String sgTwo;

    private CommandContext ctx;

    @BeforeClass
    public static void before() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");

        // deployment1
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cli-test-app1.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version0"), "page.html");
        cliTestApp1War = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(cliTestApp1War, true);

        // deployment2
        war = ShrinkWrap.create(WebArchive.class, "cli-test-app2.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version1"), "page.html");
        cliTestApp2War = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(cliTestApp2War, true);

        // deployment3
        war = ShrinkWrap.create(WebArchive.class, "cli-test-another.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version2"), "page.html");
        cliTestAnotherWar = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(cliTestAnotherWar, true);

        // deployment4
        war = ShrinkWrap.create(WebArchive.class, "cli-test-app3.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version3"), "page.html");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "cli-test-app.ear");
        ear.add(war, new BasicPath("/"), ZipExporter.class);
        cliTestAppEar = new File(tempDir + File.separator + ear.getName());
        new ZipExporterImpl(ear).exportTo(cliTestAppEar, true);

        final Iterator<String> sgI = CLITestSuite.serverGroups.keySet().iterator();
        if(!sgI.hasNext()) {
            fail("Server groups aren't available.");
        }
        sgOne = sgI.next();
        if(!sgI.hasNext()) {
            fail("Second server groups isn't available.");
        }
        sgTwo = sgI.next();
    }

    @AfterClass
    public static void after() throws Exception {
        cliTestApp1War.delete();
        cliTestApp2War.delete();
        cliTestAnotherWar.delete();
        cliTestAppEar.delete();
    }

    private Set<String> afterTestDeployments;

    @Before
    public void beforeTest() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();

        ctx.handle("deploy --server-groups=" + sgOne + ' ' + cliTestApp1War.getAbsolutePath());
        ctx.handle("deploy --server-groups=" + sgOne + ' ' + cliTestAnotherWar.getAbsolutePath());

        ctx.handle("deploy --server-groups=" + sgTwo + ' ' + cliTestApp2War.getAbsolutePath());
        ctx.handle("deploy --server-groups=" + sgTwo + ',' + sgOne + ' ' + cliTestAppEar.getAbsolutePath());

        afterTestDeployments = new HashSet<String>();
    }

    @After
    public void afterTest() throws Exception {

        StringBuilder buf = undeploy(null, cliTestApp1War.getName(), sgOne);
        buf = undeploy(buf, cliTestAnotherWar.getName(), sgOne);
        buf = undeploy(buf, cliTestApp2War.getName(), sgTwo);
        buf = undeploy(buf, cliTestAppEar.getName(), sgOne + ',' + sgTwo);

        ctx.terminateSession();
        if(buf != null) {
            fail(buf.toString());
        }
        if(afterTestDeployments.size() > 0) {
            fail("Expected to undeploy but failed to: " + afterTestDeployments);
        }
    }

    protected StringBuilder undeploy(StringBuilder buf, String deployment, String sg) {
        ctx.handleSafe("undeploy --server-groups=" + sg + ' ' + deployment);
        if(ctx.getExitCode() == 0) {
            if(!afterTestDeployments.remove(deployment)) {
                if(buf == null) {
                    buf = new StringBuilder();
                    buf.append("Undeployed unexpected content: ");
                    buf.append(deployment);
                } else {
                    buf.append(", ").append(deployment);
                }
            }
        }
        return buf;
    }

    @Test
    public void testUndeployAllWars() throws Exception {
        ctx.handle("undeploy *.war --all-relevant-server-groups");
        afterTestDeployments.add(cliTestAppEar.getName());
    }

    @Test
    public void testUndeployCliTestApps() throws Exception {
        ctx.handle("undeploy cli-test-app* --all-relevant-server-groups");
        afterTestDeployments.add(cliTestAnotherWar.getName());
    }


    @Test
    public void testUndeployTestAps() throws Exception {
        ctx.handle("undeploy *test-ap* --all-relevant-server-groups");
        afterTestDeployments.add(cliTestAnotherWar.getName());
    }

    @Test
    public void testUndeployTestAs() throws Exception {
        ctx.handle("undeploy *test-a* --all-relevant-server-groups");
    }

    @Test
    public void testUndeployTestAWARs() throws Exception {
        ctx.handle("undeploy *test-a*.war --all-relevant-server-groups");
        afterTestDeployments.add(cliTestAppEar.getName());
    }
}

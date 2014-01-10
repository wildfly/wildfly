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

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UndeployWildcardTestCase {

    @ArquillianResource URL url;

    private static File[] appFiles;

    private CommandContext ctx;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(UndeployWildcardTestCase.class);
        return ja;
    }

    @BeforeClass
    public static void before() throws Exception {
        String tempDir = TestSuiteEnvironment.getSystemProperty("java.io.tmpdir");

        appFiles = new File[4];

        // deployment1
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cli-test-app1.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version0"), "page.html");
        appFiles[0] = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(appFiles[0], true);

        // deployment2
        war = ShrinkWrap.create(WebArchive.class, "cli-test-app2.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version1"), "page.html");
        appFiles[1] = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(appFiles[1], true);

        // deployment3
        war = ShrinkWrap.create(WebArchive.class, "cli-test-another.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version2"), "page.html");
        appFiles[2] = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(appFiles[2], true);

        // deployment4
        war = ShrinkWrap.create(WebArchive.class, "cli-test-app3.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Version3"), "page.html");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "cli-test-app.ear");
        ear.add(war, new BasicPath("/"), ZipExporter.class);
        appFiles[3] = new File(tempDir + File.separator + ear.getName());
        new ZipExporterImpl(ear).exportTo(appFiles[3], true);
    }

    @AfterClass
    public static void after() throws Exception {
        for(File f : appFiles) {
            f.delete();
        }
    }

    private Set<String> afterTestDeployments;

    @Before
    public void beforeTest() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();
        for(File f : appFiles) {
            ctx.handle("deploy " + f.getAbsolutePath());
        }
        afterTestDeployments = new HashSet<String>();
    }

    @After
    public void afterTest() throws Exception {
        StringBuilder buf = null;
        for(File f : appFiles) {
            ctx.handleSafe("undeploy " + f.getName());
            if(ctx.getExitCode() == 0) {
                if(!afterTestDeployments.remove(f.getName())) {
                    if(buf == null) {
                        buf = new StringBuilder();
                        buf.append("Undeployed unexpected content: ");
                        buf.append(f.getName());
                    } else {
                        buf.append(", ").append(f.getName());
                    }
                }
            }
        }
        ctx.terminateSession();
        if(buf != null) {
            fail(buf.toString());
        }
        if(afterTestDeployments.size() > 0) {
            fail("Expected to undeploy but failed to: " + afterTestDeployments);
        }
    }

    @Test
    public void testUndeployAllWars() throws Exception {
        ctx.handle("undeploy *.war");
        afterTestDeployments.add(appFiles[3].getName());
    }

    @Test
    public void testUndeployCliTestApps() throws Exception {
        ctx.handle("undeploy cli-test-app*");
        afterTestDeployments.add(appFiles[2].getName());
    }


    @Test
    public void testUndeployTestAps() throws Exception {
        ctx.handle("undeploy *test-ap*");
        afterTestDeployments.add(appFiles[2].getName());
    }

    @Test
    public void testUndeployTestAs() throws Exception {
        ctx.handle("undeploy *test-a*");
    }

    @Test
    public void testUndeployTestAWARs() throws Exception {
        ctx.handle("undeploy *test-a*.war");
        afterTestDeployments.add(appFiles[3].getName());
    }
}

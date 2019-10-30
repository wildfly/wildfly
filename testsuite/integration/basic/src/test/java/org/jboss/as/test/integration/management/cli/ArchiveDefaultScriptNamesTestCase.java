/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.common.HttpRequest;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author btison
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ArchiveDefaultScriptNamesTestCase {

    private static File cliArchiveFile;

    @ArquillianResource URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(ArchiveDefaultScriptNamesTestCase.class);
        return ja;
    }

    @BeforeClass
    public static void before() throws Exception {
        String tempDir = TestSuiteEnvironment.getTmpDir();

        WebArchive[] wars = new WebArchive[3];

        // deployment1
        wars[0] = ShrinkWrap.create(WebArchive.class, "deployment0.war");
        wars[0].addClass(SimpleServlet.class);
        wars[0].addAsWebResource(new StringAsset("Version0"), "page.html");

        // deployment2
        wars[1] = ShrinkWrap.create(WebArchive.class, "deployment1.war");
        wars[1].addClass(SimpleServlet.class);
        wars[1].addAsWebResource(new StringAsset("Version1"), "page.html");

        // deployment3 is included but not deployed
        wars[2] = ShrinkWrap.create(WebArchive.class, "deployment2.war");
        wars[2].addClass(SimpleServlet.class);
        wars[2].addAsWebResource(new StringAsset("Version2"), "page.html");

        //build cli archive
        EnterpriseArchive cliArchive = ShrinkWrap.create(EnterpriseArchive.class, "archive.cli");
        String deploy = "deploy deployment0.war\ndeploy deployment1.war";
        String undeploy = "undeploy deployment0.war\nundeploy deployment1.war";
        cliArchive.add(new StringAsset(deploy), new BasicPath("/", "deploy.scr"));
        cliArchive.add(new StringAsset(undeploy), new BasicPath("/", "undeploy.scr"));
        for (WebArchive war : wars) {
            cliArchive.add(war, new BasicPath("/"), ZipExporter.class);
        }
        cliArchiveFile = new File(tempDir + File.separator + "archive.cli");
        new ZipExporterImpl(cliArchive).exportTo(cliArchiveFile, true);
    }

    @Test
    public void testDeployUndeployArchive() throws Exception {

        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle("deploy " + cliArchiveFile.getAbsolutePath());

            // check that now both wars are deployed
            String response = HttpRequest.get(getBaseURL(url) + "deployment0/SimpleServlet", 10, TimeUnit.SECONDS);
            assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);
            response = HttpRequest.get(getBaseURL(url) + "deployment1/SimpleServlet", 10, TimeUnit.SECONDS);
            assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);
            assertTrue(checkUndeployed(getBaseURL(url) + "deployment2/SimpleServlet"));

            ctx.handle("undeploy " + "--path=" + cliArchiveFile.getAbsolutePath());

            // check that both wars are undeployed
            assertTrue(checkUndeployed(getBaseURL(url) + "deployment0/SimpleServlet"));
            assertTrue(checkUndeployed(getBaseURL(url) + "deployment1/SimpleServlet"));
        } finally {
            ctx.terminateSession();
        }
    }

    @AfterClass
    public static void after() throws Exception {
        cliArchiveFile.delete();
    }

    protected final String getBaseURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), "/").toString();
    }

    protected boolean checkUndeployed(String spec) {
        try {
            final long firstTry = System.currentTimeMillis();
            HttpRequest.get(spec, 10, TimeUnit.SECONDS);
            while (System.currentTimeMillis() - firstTry <= 1000) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                } finally {
                    HttpRequest.get(spec, 10, TimeUnit.SECONDS);
                }
            }
            return false;
        } catch (Exception e) {
        }
        return true;
    }
}

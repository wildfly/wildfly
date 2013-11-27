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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alexey Loubyansky
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentOverlayCLITestCase {

    private static File replacedLibrary;
    private static File war1;
    private static File war2;
    private static File war3;
    private static File ear1;
    private static File webXml;
    private static File overrideXml;
    private static File replacedAjsp;

    @ArquillianResource URL url;

    private String baseUrl;

    private CommandContext ctx;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeploymentOverlayCLITestCase.class);
        return ja;
    }

    @BeforeClass
    public static void before() throws Exception {
        String tempDir = TestSuiteEnvironment.getTmpDir();

        WebArchive war;

        JavaArchive jar;

        jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.addClass(ReplacedLibraryServlet.class);
        replacedLibrary = new File(tempDir + File.separator + jar.getName());
        new ZipExporterImpl(jar).exportTo(replacedLibrary, true);

        jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.addClass(OriginalLibraryServlet.class);

        // deployment1
        war = ShrinkWrap.create(WebArchive.class, "deployment0.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(DeploymentOverlayCLITestCase.class.getPackage(), "a.jsp", "a.jsp");
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsLibraries(jar);
        war1 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war1, true);

        war = ShrinkWrap.create(WebArchive.class, "deployment1.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsLibraries(jar);
        war2 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war2, true);

        war = ShrinkWrap.create(WebArchive.class, "another.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsLibraries(jar);
        war3 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war3, true);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "eardeployment.ear");
        ear.addAsModule(war1);
        ear1 = new File(tempDir + File.separator + ear.getName());
        new ZipExporterImpl(ear).exportTo(ear1, true);

        final URL overrideXmlUrl = DeploymentOverlayCLITestCase.class.getResource("override.xml");
        if(overrideXmlUrl == null) {
            Assert.fail("Failed to locate override.xml");
        }
        overrideXml = new File(overrideXmlUrl.toURI());
        if(!overrideXml.exists()) {
            Assert.fail("Failed to locate override.xml");
        }

        final URL webXmlUrl = DeploymentOverlayCLITestCase.class.getResource("web.xml");
        if(webXmlUrl == null) {
            Assert.fail("Failed to locateweb.xml");
        }
        webXml = new File(webXmlUrl.toURI());
        if(!webXml.exists()) {
            Assert.fail("Failed to locate web.xml");
        }


        final URL ajsp = DeploymentOverlayCLITestCase.class.getResource("a-replaced.jsp");
        if(ajsp == null) {
            Assert.fail("Failed to locate a-replaced.jsp");
        }
        replacedAjsp = new File(ajsp.toURI());
        if(!replacedAjsp.exists()) {
            Assert.fail("Failed to locate a-replaced.jsp");
        }
    }

    @AfterClass
    public static void after() throws Exception {
        war1.delete();
        war2.delete();
        war3.delete();
    }

    protected final String getBaseURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), "/").toString();
    }

    @Before
    public void setUp() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();
        baseUrl = getBaseURL(url);
    }

    @After
    public void tearDown() throws Exception {
        if(ctx != null) {
            ctx.handleSafe("undeploy " + war1.getName());
            ctx.handleSafe("undeploy " + war2.getName());
            ctx.handleSafe("undeploy " + war3.getName());
            ctx.handleSafe("undeploy " + ear1.getName());
            ctx.handleSafe("deployment-overlay remove --name=overlay-test");
            ctx.terminateSession();
        }
    }

    @Test
    public void testSimpleOverride() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + ",a.jsp=" + replacedAjsp.getAbsolutePath() + ",WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                + " --deployments=" + war1.getName());

        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        //now test JSP
        assertEquals("Replaced JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());

        //now test Libraries
        assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());



    }


    @Test
    public void testSimpleOverrideInEarAtWarLevel() throws Exception {

        ctx.handle("deploy " + ear1.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + ",a.jsp=" + replacedAjsp.getAbsolutePath() + ",WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                + " --deployments=" + war1.getName());

        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + ear1.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);

        //now test JSP
        assertEquals("Replaced JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());

        //now test Libraries
        assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());

    }

    @Test
    public void testSimpleOverrideWithRedeployAffected() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=" + war1.getName() + " --redeploy-affected");

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

    }

    @Test
    public void testWildcardOverride() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=deployment*.war");

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    public void testWildcardOverrideWithRedeployAffected() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=deployment*.war --redeploy-affected");

        //Thread.sleep(2000);
        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    public void testMultipleLinks() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=" + war1.getName());

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay link --name=overlay-test --deployments=a*.war");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2.getName() + ":redeploy");
        ctx.handle("/deployment=" + war3.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("deployment-overlay link --name=overlay-test --deployments=" + war2.getName() + " --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("deployment-overlay remove --name=overlay-test --deployments=" + war2.getName() + " --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("deployment-overlay remove --name=overlay-test --deployments=a*.war");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2.getName() + ":redeploy");
        ctx.handle("/deployment=" + war3.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay remove --name=overlay-test --content=WEB-INF/web.xml --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay upload --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + " --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    public void testRedeployAffected() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath());
        ctx.handle("deployment-overlay link --name=overlay-test --deployments=deployment0.war,a*.war");

        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay redeploy-affected --name=overlay-test");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);
    }

    protected String readResponse(String warName) throws IOException, ExecutionException, TimeoutException,
            MalformedURLException {
        return HttpRequest.get(baseUrl + warName + "/SimpleServlet?env-entry=overlay-test", 10, TimeUnit.SECONDS).trim();
    }
}

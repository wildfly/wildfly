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

import org.apache.commons.io.FileUtils;
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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.ExplodedExporterImpl;
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
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentOverlayCLITestCase {

    private static File replacedLibrary;
    private static File addedLibrary;
    private static File war1;
    private static File war1_exploded;
    private static File war2;
    private static File war2_exploded;
    private static File war3;
    private static File ear1;
    private static File ear1_exploded;
    private static File ear2;
    private static File ear2_exploded;
    private static File webXml;
    private static File overrideXml;
    private static File replacedAjsp;

    @ArquillianResource
    URL url;

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
        jar.add(new StringAsset("replaced library"), "jar-info.txt");
        replacedLibrary = new File(tempDir + File.separator + jar.getName());
        new ZipExporterImpl(jar).exportTo(replacedLibrary, true);


        jar = ShrinkWrap.create(JavaArchive.class, "addedlib.jar");
        jar.addClass(AddedLibraryServlet.class);
        addedLibrary = new File(tempDir + File.separator + jar.getName());
        new ZipExporterImpl(jar).exportTo(addedLibrary, true);

        jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.addClass(OriginalLibraryServlet.class);

        // deployment1
        war = ShrinkWrap.create(WebArchive.class, "deployment0.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(DeploymentOverlayCLITestCase.class.getPackage(), "a.jsp", "a.jsp");
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsLibraries(jar);

        File explodedwars_basedir = new File(tempDir + File.separator + "exploded_deployments");
        explodedwars_basedir.mkdirs();

        war1 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war1, true);
        war1_exploded = new ExplodedExporterImpl(war).exportExploded(explodedwars_basedir);

        war = ShrinkWrap.create(WebArchive.class, "deployment1.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsLibraries(jar);
        war2 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war2, true);
        war2_exploded = new ExplodedExporterImpl(war).exportExploded(explodedwars_basedir);

        war = ShrinkWrap.create(WebArchive.class, "another.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsLibraries(jar);
        war3 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war3, true);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "eardeployment1.ear");
        ear.addAsModule(war1);
        ear1 = new File(tempDir + File.separator + ear.getName());
        new ZipExporterImpl(ear).exportTo(ear1, true);
        ear1_exploded = new ExplodedExporterImpl(ear).exportExploded(explodedwars_basedir);

        war = ShrinkWrap.create(WebArchive.class, "deployment0.war");
        war.addClass(SimpleServlet.class);
        war.addClass(EarServlet.class);
        war.addAsWebResource(DeploymentOverlayCLITestCase.class.getPackage(), "a.jsp", "a.jsp");
        war.addAsWebInfResource(DeploymentOverlayCLITestCase.class.getPackage(), "web.xml", "web.xml");

        jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.add(new StringAsset("original library"), "jar-info.txt");


        ear = ShrinkWrap.create(EnterpriseArchive.class, "eardeployment2.ear");
        ear.addAsModule(war);
        ear.addAsLibraries(jar);
        ear2 = new File(tempDir + File.separator + ear.getName());
        new ZipExporterImpl(ear).exportTo(ear2, true);
        ear2_exploded = new ExplodedExporterImpl(ear).exportExploded(explodedwars_basedir);

        final URL overrideXmlUrl = DeploymentOverlayCLITestCase.class.getResource("override.xml");
        if (overrideXmlUrl == null) {
            Assert.fail("Failed to locate override.xml");
        }
        overrideXml = new File(overrideXmlUrl.toURI());
        if (!overrideXml.exists()) {
            Assert.fail("Failed to locate override.xml");
        }

        final URL webXmlUrl = DeploymentOverlayCLITestCase.class.getResource("web.xml");
        if (webXmlUrl == null) {
            Assert.fail("Failed to locateweb.xml");
        }
        webXml = new File(webXmlUrl.toURI());
        if (!webXml.exists()) {
            Assert.fail("Failed to locate web.xml");
        }


        final URL ajsp = DeploymentOverlayCLITestCase.class.getResource("a-replaced.jsp");
        if (ajsp == null) {
            Assert.fail("Failed to locate a-replaced.jsp");
        }
        replacedAjsp = new File(ajsp.toURI());
        if (!replacedAjsp.exists()) {
            Assert.fail("Failed to locate a-replaced.jsp");
        }
    }

    @AfterClass
    public static void after() throws Exception {
        war1.delete();
        FileUtils.deleteDirectory(war1_exploded);
        war2.delete();
        FileUtils.deleteDirectory(war2_exploded);
        war3.delete();
        ear1.delete();
        FileUtils.deleteDirectory(ear1_exploded);
        ear2.delete();
        FileUtils.deleteDirectory(ear2_exploded);
        replacedLibrary.delete();
//        replacedAjsp.delete();
        addedLibrary.delete();
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
        if (ctx != null) {
            ctx.handleSafe("undeploy " + war1.getName());
            ctx.handleSafe("undeploy " + war2.getName());
            ctx.handleSafe("undeploy " + war3.getName());
            ctx.handleSafe("undeploy " + ear1.getName());
            ctx.handleSafe("undeploy " + ear2.getName());
            ctx.handleSafe("deployment-overlay remove --name=overlay-test");
            ctx.handleSafe("deployment-overlay remove --name=overlay1");
            ctx.handleSafe("deployment-overlay remove --name=overlay2");
            ctx.handleSafe("deployment-overlay remove --name=overlay3");
            ctx.terminateSession();
        }
    }

    @Test
    public void testSimpleOverride() throws Exception {
        simpleOverrideTest(false);
    }

    @Test
    public void testSimpleOverrideMultipleDeploymentOverlay() throws Exception {
        simpleOverrideTest(true);
    }

    private void simpleOverrideTest(boolean multipleOverlay) throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());

        if (multipleOverlay) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + ",a.jsp=" + replacedAjsp.getAbsolutePath() + " --deployments=" + war1.getName());
            ctx.handle("deployment-overlay add --name=overlay2 --content=WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath() + " --deployments=" + war1.getName());
            ctx.handle("deployment-overlay add --name=overlay3 --content=WEB-INF/lib/addedlib.jar=" + addedLibrary.getAbsolutePath() + " --deployments=" + war1.getName());

        } else {
            ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                    + ",a.jsp=" + replacedAjsp.getAbsolutePath() + ",WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                    + ",WEB-INF/lib/addedlib.jar=" + addedLibrary.getAbsolutePath()
                    + " --deployments=" + war1.getName());
        }

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
        assertEquals("Added Library Servlet", HttpRequest.get(baseUrl + "deployment0/AddedLibraryServlet", 10, TimeUnit.SECONDS).trim());
    }

    @Test
    public void testSimpleOverrideExploded() throws Exception {
        simpleOverrideExplodedTest(false);
    }

    @Test
    public void testSimpleOverrideExplodedMultipleDeploymentOverlay() throws Exception {
        simpleOverrideExplodedTest(true);
    }

    private void simpleOverrideExplodedTest(boolean multiple) throws Exception {

        ctx.handle("/deployment=" + war1_exploded.getName()
                + ":add(content=[{\"path\"=>\"" + war1_exploded.getAbsolutePath().replace("\\", "\\\\") + "\",\"archive\"=>false}], enabled=true)");
        ctx.handle("/deployment=" + war2_exploded.getName()
                + ":add(content=[{\"path\"=>\"" + war2_exploded.getAbsolutePath().replace("\\", "\\\\") + "\",\"archive\"=>false}], enabled=true)");

        if (multiple) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + ",a.jsp=" + replacedAjsp.getAbsolutePath() + " --deployments=" + war1_exploded.getName());
            ctx.handle("deployment-overlay add --name=overlay2 --content=WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath() + " --deployments=" + war1_exploded.getName());
            ctx.handle("deployment-overlay add --name=overlay3 --content=WEB-INF/lib/addedlib.jar=" + addedLibrary.getAbsolutePath() + " --deployments=" + war1_exploded.getName());
        } else {
            ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                    + ",a.jsp=" + replacedAjsp.getAbsolutePath() + ",WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                    + ",WEB-INF/lib/addedlib.jar=" + addedLibrary.getAbsolutePath()
                    + " --deployments=" + war1_exploded.getName());
        }


        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1_exploded.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2_exploded.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        // replacing JSP files in exploded deployments is not supported - see WFLY-2989 for more details
        // assertEquals("Replaced JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());

        //now test Libraries
        assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());
        assertEquals("Added Library Servlet", HttpRequest.get(baseUrl + "deployment0/AddedLibraryServlet", 10, TimeUnit.SECONDS).trim());
    }

    @Test
    public void testSimpleOverrideInEarAtWarLevel() throws Exception {
        simpleOverrideInEarAtWarLevelTest(false);
    }

    @Test
    public void testSimpleOverrideInEarAtWarLevelMultipleDeploymentOverlay() throws Exception {
        simpleOverrideInEarAtWarLevelTest(true);
    }

    private void simpleOverrideInEarAtWarLevelTest(boolean multiple) throws Exception {

        ctx.handle("deploy " + ear1.getAbsolutePath());

        if (multiple) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + ",a.jsp=" + replacedAjsp.getAbsolutePath() + " --deployments=" + war1.getName());
            ctx.handle("deployment-overlay add --name=overlay2 --content=WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath() + " --deployments=" + war1.getName());
        } else {
            ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                    + ",a.jsp=" + replacedAjsp.getAbsolutePath() + ",WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                    + " --deployments=" + war1.getName());
        }

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
    public void testSimpleOverrideInEarAtWarLevelExploded() throws Exception {
        simpleOverrideInEarAtWarLevelExplodedTest(false);
    }

    @Test
    public void testSimpleOverrideInEarAtWarLevelExplodedMultipleDeploymentOverlay() throws Exception {
        simpleOverrideInEarAtWarLevelExplodedTest(true);
    }


    private void simpleOverrideInEarAtWarLevelExplodedTest(boolean multiple) throws Exception {

        ctx.handle("/deployment=" + ear1_exploded.getName()
                + ":add(content=[{\"path\"=>\"" + ear1_exploded.getAbsolutePath().replace("\\", "\\\\") + "\",\"archive\"=>false}], enabled=true)");

        if (multiple) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + ",a.jsp=" + replacedAjsp.getAbsolutePath() + " --deployments=" + war1.getName());
            ctx.handle("deployment-overlay add --name=overlay2 --content=WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath() + " --deployments=" + war1.getName());
        } else {
            ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                    + ",a.jsp=" + replacedAjsp.getAbsolutePath() + ",WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                    + " --deployments=" + war1.getName());
        }

        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + ear1_exploded.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);

        //now test JSP (it works here, because only the EAR is exploded, the inner WAR is not)
        assertEquals("Replaced JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());

        //now test Libraries
        assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());

    }


    @Test
    public void testSimpleOverrideInEarAtEarLevel() throws Exception {

        ctx.handle("deploy " + ear2.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                + " --deployments=" + ear2.getName());

        //now test Libraries
        assertEquals("original library", HttpRequest.get(baseUrl + "deployment0/EarServlet", 10, TimeUnit.SECONDS).trim());

        ctx.handle("/deployment=" + ear2.getName() + ":redeploy");

        //now test Libraries
        assertEquals("replaced library", HttpRequest.get(baseUrl + "deployment0/EarServlet", 10, TimeUnit.SECONDS).trim());
    }

    @Test
    public void testSimpleOverrideInEarAtEarLevelExploded() throws Exception {

        ctx.handle("/deployment=" + ear2_exploded.getName()
                + ":add(content=[{\"path\"=>\"" + ear2_exploded.getAbsolutePath().replace("\\", "\\\\") + "\",\"archive\"=>false}], enabled=true)");


        ctx.handle("deployment-overlay add --name=overlay-test --content=lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                + " --deployments=" + ear2_exploded.getName());

        //now test Libraries
        assertEquals("original library", HttpRequest.get(baseUrl + "deployment0/EarServlet", 10, TimeUnit.SECONDS).trim());

        ctx.handle("/deployment=" + ear2_exploded.getName() + ":redeploy");

        //now test Libraries
        assertEquals("replaced library", HttpRequest.get(baseUrl + "deployment0/EarServlet", 10, TimeUnit.SECONDS).trim());
    }

    @Test
    public void testSimpleOverrideWithRedeployAffected() throws Exception {
        simpleOverrideWithRedeployAffectedTest(false);
    }

    @Test
    public void testSimpleOverrideWithRedeployAffectedMultipleDeploymentOverlay() throws Exception {
        simpleOverrideWithRedeployAffectedTest(true);
    }

    private void simpleOverrideWithRedeployAffectedTest(boolean multiple) throws Exception {
        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());

        String response1 = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response1);

        if (multiple) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=a.jsp=" + replacedAjsp.getAbsolutePath() + " --deployments=" + war1.getName());
            ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                    + " --deployments=" + war1.getName() + " --redeploy-affected");
        } else {
            ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                    + " --deployments=" + war1.getName() + " --redeploy-affected");
        }

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        if (multiple) {
            //now test JSP
            assertEquals("Replaced JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());
        }

    }

    @Test
    public void testWildcardOverride() throws Exception {
        wildcardOverrideTest(false);
    }

    @Test
    public void testWildcardOverrideMultipleDeploymentOverlay() throws Exception {
        wildcardOverrideTest(true);
    }

    private void wildcardOverrideTest(boolean multiple) throws Exception {

        if (multiple) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                    + " --deployments=deployment*.war");
        }

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

        if (multiple) {
            assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());
            assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment1/LibraryServlet", 10, TimeUnit.SECONDS).trim());
        }

    }

    @Test
    public void testWildcardOverrideWithRedeployAffected() throws Exception {
        wildcardOverrideWithRedeployAffectedTest(false);
    }

    @Test
    public void testWildcardOverrideWithRedeployAffectedMultipleDeploymentOverlay() throws Exception {
        wildcardOverrideWithRedeployAffectedTest(true);
    }

    private void wildcardOverrideWithRedeployAffectedTest(boolean multiple) throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        if (multiple) {
            ctx.handle("deployment-overlay add --name=overlay1 --content=WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath()
                    + " --deployments=deployment*.war");
        }

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=deployment*.war --redeploy-affected");

        //Thread.sleep(2000);
        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        if (multiple) {
            assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());
            assertEquals("Replaced Library Servlet", HttpRequest.get(baseUrl + "deployment1/LibraryServlet", 10, TimeUnit.SECONDS).trim());
        }
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

    @Test
    public void testSimpleOverrideRemoveOverlay() throws Exception {
        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());


        ctx.handle("deployment-overlay add --name="
                + "overlay-test --content="
                + "WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + ","
                + "a.jsp=" + replacedAjsp.getAbsolutePath() + ","
                + "WEB-INF/lib/lib.jar=" + replacedLibrary.getAbsolutePath() + ","
                + "WEB-INF/lib/addedlib.jar=" + addedLibrary.getAbsolutePath()
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
        assertEquals("Added Library Servlet", HttpRequest.get(baseUrl + "deployment0/AddedLibraryServlet", 10, TimeUnit.SECONDS).trim());


        ctx.handleSafe("deployment-overlay remove --name=overlay-test");
        ctx.handle("/deployment=" + war1.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);

        //now test Libraries
        assertEquals("Original Library Servlet", HttpRequest.get(baseUrl + "deployment0/LibraryServlet", 10, TimeUnit.SECONDS).trim());
        try {
//            Assert.assertNotEquals("Added Library Servlet", HttpRequest.get(baseUrl + "deployment0/AddedLibraryServlet", 10, TimeUnit.SECONDS).trim());
            HttpRequest.get(baseUrl + "deployment0/AddedLibraryServlet", 10, TimeUnit.SECONDS);
            Assert.fail();
        } catch (IOException e) {
            //ok
        }
        //now test JSP
        assertEquals("Original JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());
    }

    @Test
    public void testSimpleOverrideRemoveOverlay2() throws Exception {
        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());


        ctx.handle("deployment-overlay add --name="
                + "overlay-test --content="
                + "a.jsp=" + replacedAjsp.getAbsolutePath() + ","
                + " --deployments=" + war1.getName());


        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);

        //now test JSP
        assertEquals("Replaced JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());

        ctx.handleSafe("deployment-overlay remove --name=overlay-test");
        ctx.handle("/deployment=" + war1.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);

        //now test JSP
        assertEquals("Original JSP File", HttpRequest.get(baseUrl + "deployment0/a.jsp", 10, TimeUnit.SECONDS).trim());
    }


    protected String readResponse(String warName) throws IOException, ExecutionException, TimeoutException {
        return HttpRequest.get(baseUrl + warName + "/SimpleServlet?env-entry=overlay-test", 10, TimeUnit.SECONDS).trim();
    }
}

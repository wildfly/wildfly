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
package org.jboss.as.test.integration.management.cli;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.SimpleHelloWorldServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeployWithRuntimeNameTestCase {

    @ArquillianResource
    URL url;


    private CommandContext ctx;
    private File warFile;
    private String baseUrl;

    public static final String RUNTIME_NAME = "SimpleServlet.war";
    public static final String OTHER_RUNTIME_NAME = "OtherSimpleServlet.war";
    private static final String APP_NAME = "simple1";
    private static final String OTHER_APP_NAME = "simple2";

     @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar").addClass(DeployWithRuntimeNameTestCase.class);
    }

    @BeforeClass
    public static void setupCli() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @Before
    public void setUp() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();
        baseUrl = getBaseURL(url);
    }

    private File createWarFile(String content) throws IOException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "HelloServlet.war");
        war.addClass(SimpleHelloWorldServlet.class);
        war.addAsWebInfResource(SimpleHelloWorldServlet.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(new StringAsset(content), "page.html");
        File tempFile = new File(TestSuiteEnvironment.getTmpDir(), "HelloServlet.war");
        new ZipExporterImpl(war).exportTo(tempFile, true);
        return tempFile;
    }

    @AfterClass
    public static void cleanup() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @After
    public void undeployAll() {
        assertThat(warFile.delete(), is(true));
        if(ctx != null) {
            ctx.handleSafe("undeploy " + APP_NAME);
            ctx.handleSafe("undeploy " +OTHER_APP_NAME);
            ctx.terminateSession();
        }
    }

    @Test
    public void testDeployWithSameRuntimeName() throws Exception {
        warFile = createWarFile("Version1");
        ctx.handle(buildDeployCommand(RUNTIME_NAME, APP_NAME));
        checkURL("SimpleServlet/page.html", "Version1",  false);
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet", false);
        warFile = createWarFile("Shouldn't be deployed, as runtime already exist");
        try {
        ctx.handle(buildDeployCommand(RUNTIME_NAME, OTHER_APP_NAME));
        } catch(CommandLineException ex) {
            assertThat(ex.getMessage(), containsString("WFLYSRV0205"));
        }
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet",  false);
        checkURL("SimpleServlet/page.html", "Version1",  false);
    }

    @Test
    public void testDeployWithDifferentRuntimeName() throws Exception {
        warFile = createWarFile("Version1");
        ctx.handle(buildDeployCommand(RUNTIME_NAME, APP_NAME));
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet",  false);
        checkURL("SimpleServlet/page.html", "Version1",  false);
        warFile = createWarFile("Version2");
        ctx.handle(buildDeployCommand(OTHER_RUNTIME_NAME, OTHER_APP_NAME));
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet",  false);
        checkURL("SimpleServlet/page.html", "Version1", false);
        checkURL("OtherSimpleServlet/hello", "SimpleHelloWorldServlet", false);
        checkURL("OtherSimpleServlet/page.html", "Version2",false);
    }

    @Test
    public void testUndeployWithDisabledSameRuntimeName() throws Exception {
        warFile = createWarFile("Version1");
        ctx.handle(buildDeployCommand(RUNTIME_NAME, APP_NAME));
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet", false);
        checkURL("SimpleServlet/page.html", "Version1", false);
        warFile = createWarFile("Version2");
        ctx.handle(buildDisabledDeployCommand(RUNTIME_NAME, OTHER_APP_NAME));
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet", false);
        checkURL("SimpleServlet/page.html", "Version1", false);
        ctx.handle(buildUndeployCommand(OTHER_APP_NAME));
        checkURL("SimpleServlet/hello", "SimpleHelloWorldServlet", false);
        checkURL("SimpleServlet/page.html", "Version1", false);
    }

    private String buildDeployCommand(String runtimeName, String name) {
        return "deploy " + warFile.getAbsolutePath() + " --runtime-name=" + runtimeName + " --name=" + name;
    }

    private String buildDisabledDeployCommand(String runtimeName, String name) {
        return "deploy " + warFile.getAbsolutePath() + " --runtime-name=" + runtimeName + " --name=" + name + " --disabled";
    }

    private String buildUndeployCommand(String name) {
        return "/deployment=" + name + ":undeploy()";
    }

    private void checkURL(String path, String content, boolean shouldFail) throws Exception {
        boolean failed = false;
        try {
            String response = HttpRequest.get(baseUrl + path, 10, TimeUnit.SECONDS);
            assertThat(response, containsString(content));
        } catch (Exception e) {
            failed = true;
            if (!shouldFail) {
                throw new Exception("Http request failed.", e);
            }
        }
        if (shouldFail) {
            assertThat(baseUrl + path, failed, is(true));
        }
    }

    private String getBaseURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), "/").toString();
    }
}

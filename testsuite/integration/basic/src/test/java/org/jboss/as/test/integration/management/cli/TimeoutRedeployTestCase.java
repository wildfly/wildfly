/*
 * Copyright (C) 2013 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.cli.session.LongProcessingServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TimeoutRedeployTestCase extends AbstractCliTestBase {

    private static final String DEPLOYMENT = "timeout";
    private static File warFile = new File(System.getProperty("java.io.tmpdir"), DEPLOYMENT + ".war");

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeployTestCase.class);
        return ja;
    }

    @BeforeClass
    public static void setupCli() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    private void createWarFile(String content) throws IOException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(LongProcessingServlet.class);
        war.addAsWebInfResource(LongProcessingServlet.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(LongProcessingServlet.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.add(new StringAsset("<%@page contentType=\"text/html\" pageEncoding=\"UTF-8\"%>\n"
                + "<!DOCTYPE html>\n" + "<html>\n" + "    <head>\n"
                + "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
                + "        <title>Long Processing " + content + "</title>\n" + "    </head>\n" + "    <body>\n"
                + "        <h1>Long Processing " + content + "</h1>\n"
                + "    </body>\n</html>"), "index" + content + ".jsp");
        new ZipExporterImpl(war).exportTo(warFile, true);
    }

    @Test
    public void testRedeployementTimeout() throws Exception {
        createWarFile("1");
        cli.sendLine("deploy " + warFile.getAbsolutePath());
        String response = HttpRequest.get(getBaseURL(url) + DEPLOYMENT + "/index1.jsp", 10000, 10, TimeUnit.SECONDS);
        assertThat("Invalid response: " + response, response, containsString("Long Processing 1"));
        Thread longProcessingRequest = new Thread(new Runnable() {
            public void run() {
                try {
                    HttpRequest.get(getBaseURL(url) + DEPLOYMENT + "/process?timeout=100000", 100000, 100, TimeUnit.SECONDS);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                } catch (TimeoutException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        longProcessingRequest.start();
        createWarFile("2");
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --name=timeout.war --runtime-name=timeout.war --force=true");
        response = HttpRequest.get(getBaseURL(url) + DEPLOYMENT + "/index2.jsp", 20000, 100, TimeUnit.SECONDS);
        assertThat("Invalid response: " + response, response, containsString("Long Processing 2"));
        assertThat(longProcessingRequest.isAlive(), is(true));
        longProcessingRequest.interrupt();
    }

    @After
    public void undeploy() {
        cli.sendLine("undeploy " + DEPLOYMENT + ".war");
        warFile.delete();
    }

}

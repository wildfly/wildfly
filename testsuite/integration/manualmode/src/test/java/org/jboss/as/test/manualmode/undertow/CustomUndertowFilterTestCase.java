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
 *
 */
package org.jboss.as.test.manualmode.undertow;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * Tests custom http handler(s) configured as {@code custom-filter}s in the undertow subsystem
 *
 * @author Jaikiran Pai
 */
@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class CustomUndertowFilterTestCase {

    private static final Logger logger = Logger.getLogger(CustomUndertowFilterTestCase.class);

    private static final String CUSTOM_FILTER_MODULE_NAME = "custom-undertow-filter-module";
    private static final String CUSTOM_FILTER_CLASSNAME = CustomHttpHandler.class.getName();
    private static final String CUSTOM_FILTER_RESOURCE_NAME = "testcase-added-custom-undertow-filter";
    private static final String WAR_DEPLOYMENT_NAME = "test-tccl-in-custom-undertow-handler-construction";

    @Inject
    private static ServerController CONTROLLER;

    @BeforeClass
    public static void setupServer() throws Exception {
        CONTROLLER.start();
        // setup the server with the necessary Undertow filter configurations
        prepareServerConfiguration();
        // deploy an web application
        deploy();
    }

    @AfterClass
    public static void resetServer() throws Exception {
        try {
            undeploy();
        } catch (Exception e) {
            // ignore
            logger.debug("Ignoring exception that occurred during un-deploying", e);
        }
        try {
            // cleanup the undertow configurations we did in this test
            resetServerConfiguration();
        } finally {
            CONTROLLER.stop();
        }
    }

    private static void prepareServerConfiguration() throws Exception {
        try (final CLIWrapper cli = new CLIWrapper(true)) {
            // create a (JBoss) module jar containing the custom http handler and a dummy class that gets used
            // by the handler
            final Path moduleJar = createJar("custom-http-handler", CustomHttpHandler.class, SomeClassInSameModuleAsCustomHttpHandler.class);
            try {
                cli.sendLine("module add --name=" + CUSTOM_FILTER_MODULE_NAME
                        + " --slot=main --dependencies=io.undertow.core --resources="
                        + moduleJar.toAbsolutePath());
            } finally {
                Files.deleteIfExists(moduleJar);
            }
            // create a custom-filter in the undertow subsystem and use the filter class that's
            // present in the module that we just created
            cli.sendLine(String.format(
                    "/subsystem=undertow/configuration=filter/custom-filter=%s:add(class-name=%s, module=%s)",
                    CUSTOM_FILTER_RESOURCE_NAME, CUSTOM_FILTER_CLASSNAME, CUSTOM_FILTER_MODULE_NAME));
            // add a reference to this custom filter, in the default undertow host, so that it gets used
            // for all deployed applications on this host
            cli.sendLine(String.format(
                    "/subsystem=undertow/server=default-server/host=default-host/filter-ref=%s:add()",
                    CUSTOM_FILTER_RESOURCE_NAME));
            // reload the server for changes to take effect
            ServerReload.executeReloadAndWaitForCompletion(CONTROLLER.getClient().getControllerClient());
        }
    }

    private static void resetServerConfiguration() throws Exception {
        try (final CLIWrapper cli = new CLIWrapper(true)) {
            // remove the filter-ref
            cli.sendLine(String.format(
                    "/subsystem=undertow/server=default-server/host=default-host/filter-ref=%s:remove()",
                    CUSTOM_FILTER_RESOURCE_NAME));
            // remove the custom undertow filter
            cli.sendLine(String.format(
                    "/subsystem=undertow/configuration=filter/custom-filter=%s:remove()",
                    CUSTOM_FILTER_RESOURCE_NAME));
            // remove the module containing the filter
            cli.sendLine("module remove --name=" + CUSTOM_FILTER_MODULE_NAME);
            // reload the server
            ServerReload.executeReloadAndWaitForCompletion(CONTROLLER.getClient().getControllerClient());
        }
    }

    private static void deploy() throws Exception {
        // create a deployment
        final Path warFilePath = createDeployment();
        // deploy it to the server
        try (final CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format(
                    "deploy %s --name=%s",
                    warFilePath.toAbsolutePath().toString(), WAR_DEPLOYMENT_NAME + ".war"));
        }

    }

    private static void undeploy() throws Exception {
        try (final CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format(
                    "undeploy %s", WAR_DEPLOYMENT_NAME + ".war"));
        }
    }

    /**
     * Tests that the {@link Thread#getContextClassLoader() TCCL} that's set when a
     * custom {@link io.undertow.server.HttpHandler}, part of a (JBoss) module, configured in the undertow subsystem
     * is initialized/constructed, the classloader is the same as the classloader of the module to which
     * the handler belongs
     */
    @Test
    public void testTCCLInHttpHandlerInitialization() throws Exception {
        final String url = "http://" + TestSuiteEnvironment.getHttpAddress()
                + ":" + TestSuiteEnvironment.getHttpPort()
                + "/" + WAR_DEPLOYMENT_NAME + "/index.html";
        logger.debug("Invoking request at " + url);
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet httpget = new HttpGet(url);
            final HttpResponse response = httpClient.execute(httpget);
            final StatusLine statusLine = response.getStatusLine();
            assertEquals("Unexpected HTTP response status code for request " + url, 200, statusLine.getStatusCode());

            // make sure the custom http handler was invoked and it was initialized with the right TCCL
            final Header[] headerOneValues = response.getHeaders(CustomHttpHandler.RESPONSE_HEADER_ONE_NAME);
            Assert.assertEquals("Unexpected number of response header value for header " + CustomHttpHandler.RESPONSE_HEADER_ONE_NAME, 1, headerOneValues.length);
            Assert.assertEquals("Unexpected response header value for header " + CustomHttpHandler.RESPONSE_HEADER_ONE_NAME, true, Boolean.valueOf(headerOneValues[0].getValue()));

            final Header[] headerTwoValues = response.getHeaders(CustomHttpHandler.RESPONSE_HEADER_TWO_NAME);
            Assert.assertEquals("Unexpected number of response header value for header " + CustomHttpHandler.RESPONSE_HEADER_TWO_NAME, 1, headerTwoValues.length);
            Assert.assertEquals("Unexpected response header value for header " + CustomHttpHandler.RESPONSE_HEADER_TWO_NAME, true, Boolean.valueOf(headerTwoValues[0].getValue()));
        }
    }


    private static Path createJar(final String namePrefix, final Class<?>... classes) throws IOException {
        final Path jarFilePath = Files.createTempFile(namePrefix, ".jar");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class).addClasses(classes);
        jar.as(ZipExporter.class).exportTo(jarFilePath.toFile(), true);
        return jarFilePath;
    }

    private static Path createDeployment() throws IOException {
        final Path destWarFilePath = Files.createTempFile(null, ".war");
        final WebArchive war = ShrinkWrap.create(WebArchive.class).addAsWebResource(new StringAsset("Hello world!"), "index.html");
        war.as(ZipExporter.class).exportTo(destWarFilePath.toFile(), true);
        return destWarFilePath;
    }
}

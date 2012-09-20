/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.osgi.simple;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.simple.bundleA.ServletV100;
import org.jboss.as.test.integration.osgi.simple.bundleA.ServletV101;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test simple OSGi bundle redeployment
 *
 * @author thomas.diesler@jboss.com
 * @since 29-Aug-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SimpleBundleReplaceTestCase {

    static final String SIMPLE_BUNDLE_WAB = "simple-bundle.wab";
    static final String UPDATED_BUNDLE_WAB = "updated-bundle.wab";

    static final Asset ASSET_V100 = new StringAsset("Resource V1.0.0");
    static final Asset ASSET_V101 = new StringAsset("Resource V1.0.1");

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar");
    }

    @Deployment(name = SIMPLE_BUNDLE_WAB, managed = false, testable = false)
    public static Archive<?> getBundleWithWabExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SIMPLE_BUNDLE_WAB);
        archive.addClasses(ServletV100.class);
        archive.addAsResource(ASSET_V100, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(SIMPLE_BUNDLE_WAB);
                builder.addBundleVersion("1.0.0");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebServlet.class, Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/simple-bundle");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = UPDATED_BUNDLE_WAB, managed = false, testable = false)
    public static JavaArchive getGoodBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, UPDATED_BUNDLE_WAB);
        archive.addClasses(ServletV101.class);
        archive.addAsResource(ASSET_V101, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(SIMPLE_BUNDLE_WAB);
                builder.addBundleVersion("1.0.1");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebServlet.class, Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/simple-bundle");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleReplace() throws Exception {

        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        InputStream input = deployer.getDeployment(SIMPLE_BUNDLE_WAB);
        String runtimeName = server.deploy(SIMPLE_BUNDLE_WAB, input);

        String result = performCall("simple-bundle", "simple", null);
        Assert.assertEquals("ServletV100", result);
        result = performCall("simple-bundle", "message.txt", null);
        Assert.assertEquals("Resource V1.0.0", result);

        input = deployer.getDeployment(UPDATED_BUNDLE_WAB);
        runtimeName = server.replace(UPDATED_BUNDLE_WAB, SIMPLE_BUNDLE_WAB, input, true);
        try {
            result = performCall("simple-bundle", "simple", null);
            Assert.assertEquals("ServletV101", result);
            result = performCall("simple-bundle", "message.txt", null);
            Assert.assertEquals("Resource V1.0.1", result);
        } finally {
            server.undeploy(runtimeName);
        }
    }

    private String performCall(String context, String pattern, String param) throws Exception {
        String urlspec = managementClient.getWebUri() + "/" + context + "/";
        URL url = new URL(urlspec + pattern + (param != null ? "?input=" + param : ""));
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}

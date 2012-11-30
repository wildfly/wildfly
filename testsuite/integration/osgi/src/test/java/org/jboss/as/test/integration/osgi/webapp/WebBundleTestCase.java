/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.webapp;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.webapp.bundle.WebBundleServlet;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Test webapp deployemnts as OSGi bundles
 *
 * @author thomas.diesler@jboss.com
 *
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
public class WebBundleTestCase {

    static final String SIMPLE_WAR = "simple.war";

    static final Asset STRING_ASSET = new StringAsset("Hello from Resource");

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "webbundle-tests");
        jar.addClasses(HttpRequest.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ManagementClient.class, ServerDeploymentHelper.class);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Test
    public void testWarDeployment() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            String result = performCall("/simple/servlet?input=Hello");
            Assert.assertEquals("Hello", result);
            // Test resource access
            result = performCall("/simple/message.txt");
            Assert.assertEquals("Hello from Resource", result);
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    @Test
    public void testWarDeploymentThroughBundleContext() throws Exception {
        InputStream input = deployer.getDeployment(SIMPLE_WAR);
        Bundle bundle = context.installBundle("webbundle://simple?Bundle-SymbolicName=com.example", input);
        try {
            bundle.start();
            String result = performCall("/simple/servlet?input=Hello");
            Assert.assertEquals("Hello from com.example", result);
            // Test resource access
            result = performCall("/simple/message.txt");
            Assert.assertEquals("Hello from Resource", result);
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testWarDeploymentThroughManagement() throws Exception {
        InputStream input = deployer.getDeployment(SIMPLE_WAR);
        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        String runtimeName = server.deploy("webbundle://simple?Bundle-SymbolicName=com.example", input);
        try {
            String result = performCall("/simple/servlet?input=Hello");
            Assert.assertEquals("Hello from com.example", result);
            // Test resource access
            result = performCall("/simple/message.txt");
            Assert.assertEquals("Hello from Resource", result);
        } finally {
            server.undeploy(runtimeName);
        }
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 5, TimeUnit.SECONDS);
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(WebBundleServlet.class);
        archive.addAsWebResource(STRING_ASSET, "message.txt");
        return archive;
    }
}

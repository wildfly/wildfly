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
package org.jboss.as.test.integration.osgi.ear;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.api.Echo;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test OSGi bundles contained in EARs
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
public class EnterpriseArchiveTestCase {

    private static final String SIMPLE_EAR = "simple.ear";
    private static final String SIMPLE_WAR = "simple.war";

    private static final String WAR_STRUCTURE_EAR = "war-structure.ear";
    private static final String WAR_STRUCTURE_BUNDLE = "war-structure-bundle.war";

    private static final String ECHO_BUNDLE = "echo-bundle.jar";

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> testDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-ear-test");
        jar.addClasses(HttpRequest.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ManagementClient.class);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Deployment(name = SIMPLE_EAR, testable = false)
    public static Archive<?> getSimpleEar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        war.addClasses(SimpleServlet.class, Echo.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsModule(war);
        return ear;
    }

    @Deployment(name = WAR_STRUCTURE_EAR, testable = false)
    public static Archive<?> getWarStructureEar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_STRUCTURE_BUNDLE);
        war.addClasses(SimpleServlet.class);
        war.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(war.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addImportPackages(Echo.class);
                builder.addBundleClasspath("WEB-INF/classes");
                return builder.openStream();
            }
        });

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ECHO_BUNDLE);
        jar.addClasses(Echo.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages(Echo.class);
                return builder.openStream();
            }
        });

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WAR_STRUCTURE_EAR);
        ear.add(jar, "/", ZipExporter.class);
        ear.addAsModule(war);
        return ear;
    }

    @Test
    public void testSimpleEar() throws Exception {
        String result = performCall("/simple/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    public void testWarStructureEar() throws Exception {
        String result = performCall("/war-structure-bundle/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 10, TimeUnit.SECONDS);
    }
}

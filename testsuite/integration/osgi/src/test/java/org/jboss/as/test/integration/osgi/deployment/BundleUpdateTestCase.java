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
package org.jboss.as.test.integration.osgi.deployment;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.deployment.bundle.ServletV100;
import org.jboss.as.test.integration.osgi.deployment.bundle.ServletV101;
import org.jboss.as.test.integration.osgi.deployment.bundle.ServletV200;
import org.jboss.as.test.integration.osgi.deployment.suba.ResourceRevisionAccess;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Test simple OSGi bundle update
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Mar-2013
 */
@RunWith(Arquillian.class)
public class BundleUpdateTestCase {

    static final String BUNDLE_V100_WAB = "webapp-v100.wab";
    static final String BUNDLE_V101_WAB = "webapp-v101.wab";
    static final String BUNDLE_V100_JAR = "bundle-v100.jar";
    static final String BUNDLE_V101_JAR = "bundle-v101.jar";
    static final String BUNDLE_V200_WAB = "bundle-v200.wab";
    static final String V200_JAR = "v200.jar";
    static final String V201_JAR = "v201.jar";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static Archive<?> getDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deployment-update-tests");
        archive.addClasses(ServerDeploymentHelper.class, HttpRequest.class, FrameworkUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ClientConstants.class, ModelControllerClient.class, ManagementClient.class, DeploymentPlanBuilder.class);
                builder.addImportPackages(FrameworkWiring.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSimpleBundleUpdate() throws Exception {
        Bundle bundleA = context.installBundle(BUNDLE_V100_JAR, deployer.getDeployment(BUNDLE_V100_JAR));
        try {
            BundleRevisions brevs = bundleA.adapt(BundleRevisions.class);
            Assert.assertEquals(1, brevs.getRevisions().size());

            bundleA.update(deployer.getDeployment(BUNDLE_V101_JAR));
            Assert.assertEquals(2, brevs.getRevisions().size());
        } finally {
            bundleA.uninstall();
        }
    }

    @Test
    public void testActiveBundleUpdate() throws Exception {
        Bundle bundleA = context.installBundle(BUNDLE_V100_JAR, deployer.getDeployment(BUNDLE_V100_JAR));
        try {
            bundleA.start();
            Assert.assertEquals(Bundle.ACTIVE, bundleA.getState());

            BundleRevisions brevs = bundleA.adapt(BundleRevisions.class);
            Assert.assertEquals(1, brevs.getRevisions().size());

            bundleA.update(deployer.getDeployment(BUNDLE_V101_JAR));
            Assert.assertEquals(2, brevs.getRevisions().size());
            Assert.assertEquals(Bundle.ACTIVE, bundleA.getState());
        } finally {
            bundleA.uninstall();
        }
    }

    @Test
    public void testDependentJarUpdate() throws Exception {
        Bundle bundleA = context.installBundle(V200_JAR, deployer.getDeployment(V200_JAR));
        Bundle bundleB = context.installBundle(BUNDLE_V200_WAB, deployer.getDeployment(BUNDLE_V200_WAB));
        try {
            bundleB.start();

            String result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);

            bundleA.update(deployer.getDeployment(V201_JAR));

            // The wiring should not be effected
            result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);

            bundleB.uninstall();
            bundleB = context.installBundle(BUNDLE_V200_WAB, deployer.getDeployment(BUNDLE_V200_WAB));
            bundleB.start();

            // The wiring should have changed
            result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200-rev1.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);
        } finally {
            bundleB.uninstall();
            bundleA.uninstall();
        }
    }

    @Test
    @Ignore
    public void testWebAppBundleUpdate() throws Exception {
        Bundle bundleA = context.installBundle(BUNDLE_V100_WAB, deployer.getDeployment(BUNDLE_V100_WAB));
        try {
            bundleA.start();

            String result = performCall("simple-bundle", "simple", null);
            Assert.assertEquals("ServletV100", result);
            result = performCall("simple-bundle", "message.txt", null);
            Assert.assertEquals("Resource V1.0.0", result);

            bundleA.update(deployer.getDeployment(BUNDLE_V101_WAB));
            result = performCall("simple-bundle", "simple", null);
            Assert.assertEquals("ServletV101", result);
            result = performCall("simple-bundle", "message.txt", null);
            Assert.assertEquals("Resource V1.0.1", result);
        } finally {
            bundleA.uninstall();
        }
    }

    private String performCall(String context, String pattern, String param) throws Exception {
        String urlspec = managementClient.getWebUri() + "/" + context + "/";
        URL url = new URL(urlspec + pattern + (param != null ? "?input=" + param : ""));
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    @Deployment(name = BUNDLE_V100_WAB, managed = false, testable = false)
    public static Archive<?> getWebApp100() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_V100_WAB);
        archive.addClasses(ServletV100.class);
        archive.addAsResource(new StringAsset("Resource V1.0.0"), "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(BUNDLE_V100_WAB);
                builder.addBundleVersion("1.0.0");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebServlet.class, Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/simple-bundle");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_V101_WAB, managed = false, testable = false)
    public static JavaArchive getWebApp101() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_V101_WAB);
        archive.addClasses(ServletV101.class);
        archive.addAsResource(new StringAsset("Resource V1.0.1"), "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(BUNDLE_V101_WAB);
                builder.addBundleVersion("1.0.1");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebServlet.class, Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/simple-bundle");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_V100_JAR, managed = false, testable = false)
    public static JavaArchive getBundleV200() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_V100_JAR);
        archive.addClasses(ResourceRevisionAccess.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(BUNDLE_V100_JAR);
                builder.addBundleVersion("1.0.0");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleReference.class);
                builder.addExportPackages(ResourceRevisionAccess.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_V101_JAR, managed = false, testable = false)
    public static JavaArchive getBundleV201() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_V101_JAR);
        archive.addClasses(ResourceRevisionAccess.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(BUNDLE_V101_JAR);
                builder.addBundleVersion("1.0.1");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleReference.class);
                builder.addExportPackages(ResourceRevisionAccess.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_V200_WAB, managed = false, testable = false)
    public static Archive<?> getWebV200War() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_V200_WAB);
        archive.addClasses(ServletV200.class);
        archive.addAsResource(new StringAsset("Resource V2.0.0"), "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(BUNDLE_V200_WAB);
                builder.addBundleVersion("2.0.0");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleReference.class);
                builder.addImportPackages(ResourceRevisionAccess.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = V200_JAR, managed = false, testable = false)
    public static JavaArchive getV200Jar() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, V200_JAR);
        archive.addClasses(ResourceRevisionAccess.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(V200_JAR);
                builder.addBundleVersion("2.0.0");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ModuleClassLoader.class);
                builder.addExportPackages(ResourceRevisionAccess.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = V201_JAR, managed = false, testable = false)
    public static JavaArchive getV201Jar() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, V201_JAR);
        archive.addClasses(ResourceRevisionAccess.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(V201_JAR);
                builder.addBundleVersion("2.0.1");
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ModuleClassLoader.class);
                builder.addExportPackages(ResourceRevisionAccess.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

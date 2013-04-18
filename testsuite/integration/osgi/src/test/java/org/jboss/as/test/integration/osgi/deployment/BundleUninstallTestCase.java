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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.deployment.bundle.ServletV200;
import org.jboss.as.test.integration.osgi.deployment.suba.ResourceRevisionAccess;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Test OSGi bundle uninstall
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Mar-2013
 */
@RunWith(Arquillian.class)
public class BundleUninstallTestCase {

    static final String BUNDLE_V200_WAB = "bundle-v200.wab";
    static final String V200_JAR = "v200.jar";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static Archive<?> getDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deployment-uninstall-tests");
        archive.addClasses(ServerDeploymentHelper.class, HttpRequest.class, FrameworkUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ClientConstants.class, ModelControllerClient.class, ManagementClient.class, DeploymentPlanBuilder.class);
                builder.addImportPackages(FrameworkWiring.class, XResource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @InSequence(1)
    public void testSimpleJarUninstall() throws Exception {
        Bundle jar = context.installBundle(V200_JAR, deployer.getDeployment(V200_JAR));
        Assert.assertEquals(Bundle.INSTALLED, jar.getState());

        XEnvironment env = context.getBundle().adapt(XEnvironment.class);
        XBundleRevision brev = (XBundleRevision) jar.adapt(BundleRevision.class);
        Long resid = brev.getAttachment(XResource.RESOURCE_IDENTIFIER_KEY);

        jar.uninstall();
        Assert.assertEquals(Bundle.UNINSTALLED, jar.getState());

        Assert.assertNull("BundleRevision is null", jar.adapt(BundleRevision.class));
        Assert.assertNull("BundleWiring is null", brev.getWiring());
        Assert.assertNull("BundleRevision removed from environment", env.getResourceById(resid));
    }

    @Test
    @InSequence(20)
    public void testActiveJarUninstall() throws Exception {
        Bundle jar = context.installBundle(V200_JAR, deployer.getDeployment(V200_JAR));
        jar.start();
        Assert.assertEquals(Bundle.ACTIVE, jar.getState());

        XEnvironment env = context.getBundle().adapt(XEnvironment.class);
        XBundleRevision brev = (XBundleRevision) jar.adapt(BundleRevision.class);
        Long resid = brev.getAttachment(XResource.RESOURCE_IDENTIFIER_KEY);

        jar.uninstall();
        Assert.assertEquals(Bundle.UNINSTALLED, jar.getState());

        Assert.assertNull("BundleRevision is null", jar.adapt(BundleRevision.class));
        Assert.assertNull("BundleWiring is null", brev.getWiring());
        Assert.assertNull("BundleRevision removed from environment", env.getResourceById(resid));
    }

    @Test
    @InSequence(30)
    public void testSimpleJarUndeploy() throws Exception {
        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        String jarName = server.deploy(V200_JAR, deployer.getDeployment(V200_JAR));
        Bundle jar = FrameworkUtils.getBundles(context, V200_JAR, null)[0];
        Assert.assertEquals(Bundle.ACTIVE, jar.getState());

        XEnvironment env = context.getBundle().adapt(XEnvironment.class);
        XBundleRevision brev = (XBundleRevision) jar.adapt(BundleRevision.class);
        Long resid = brev.getAttachment(XResource.RESOURCE_IDENTIFIER_KEY);

        server.undeploy(jarName);
        Assert.assertEquals(Bundle.UNINSTALLED, jar.getState());

        Assert.assertNull("BundleRevision is null", jar.adapt(BundleRevision.class));
        Assert.assertNull("BundleWiring is null", brev.getWiring());
        Assert.assertNull("BundleRevision removed from environment", env.getResourceById(resid));
    }

    @Test
    @InSequence(40)
    public void testSimpleJarRedeploy() throws Exception {
        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        String jarName = server.deploy(V200_JAR, deployer.getDeployment(V200_JAR));
        Bundle jarA = FrameworkUtils.getBundles(context, V200_JAR, null)[0];
        Assert.assertEquals(Bundle.ACTIVE, jarA.getState());

        server.undeploy(jarName);
        Assert.assertEquals(Bundle.UNINSTALLED, jarA.getState());

        jarName = server.deploy(V200_JAR, deployer.getDeployment(V200_JAR));
        Bundle jarB = FrameworkUtils.getBundles(context, V200_JAR, null)[0];
        Assert.assertEquals(Bundle.ACTIVE, jarB.getState());

        server.undeploy(jarName);
        Assert.assertEquals(Bundle.UNINSTALLED, jarB.getState());
    }

    @Test
    @InSequence(50)
    public void testDependentJarUninstall() throws Exception {
        Bundle jar = context.installBundle(V200_JAR, deployer.getDeployment(V200_JAR));
        Bundle webapp = context.installBundle(BUNDLE_V200_WAB, deployer.getDeployment(BUNDLE_V200_WAB));

        XEnvironment env = context.getBundle().adapt(XEnvironment.class);
        XBundleRevision brev = (XBundleRevision) jar.adapt(BundleRevision.class);
        Long resid = brev.getAttachment(XResource.RESOURCE_IDENTIFIER_KEY);
        try {
            webapp.start();

            String result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);

            jar.uninstall();
            Assert.assertEquals(Bundle.UNINSTALLED, jar.getState());

            Assert.assertNull("BundleRevision is null", jar.adapt(BundleRevision.class));
            Assert.assertTrue("BundleWiring in use", brev.getWiring().isInUse());
            Assert.assertFalse("BundleWiring not current", brev.getWiring().isCurrent());
            Assert.assertSame(brev, env.getResourceById(resid));

            // The wiring should not be effected
            result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);
        } finally {
            webapp.uninstall();
        }

        // Assert that the jar revision was removed from the environment
        Assert.assertNull("BundleRevision removed", env.getResourceById(resid));
        Assert.assertNull("BundleWiring null", brev.getWiring());
    }

    @Test
    @InSequence(60)
    public void testDependentJarUndeploy() throws Exception {
        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        String jarName = server.deploy(V200_JAR, deployer.getDeployment(V200_JAR));
        String webappName = server.deploy(BUNDLE_V200_WAB, deployer.getDeployment(BUNDLE_V200_WAB));

        XEnvironment env = context.getBundle().adapt(XEnvironment.class);
        Bundle jar = FrameworkUtils.getBundles(context, V200_JAR, null)[0];
        XBundleRevision brev = (XBundleRevision) jar.adapt(BundleRevision.class);
        Long resid = brev.getAttachment(XResource.RESOURCE_IDENTIFIER_KEY);
        try {
            String result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);

            server.undeploy(jarName);
            Assert.assertEquals(Bundle.UNINSTALLED, jar.getState());

            Assert.assertNull("BundleRevision is null", jar.adapt(BundleRevision.class));
            Assert.assertTrue("BundleWiring in use", brev.getWiring().isInUse());
            Assert.assertFalse("BundleWiring not current", brev.getWiring().isCurrent());
            Assert.assertSame(brev, env.getResourceById(resid));

            // The wiring should not be effected
            result = performCall("bundle-v200", "simple", null);
            Assert.assertEquals("Revision deployment.v200.jar:main", result);
            result = performCall("bundle-v200", "message.txt", null);
            Assert.assertEquals("Resource V2.0.0", result);
        } finally {
            server.undeploy(webappName);
        }

        // Assert that the jar revision was removed from the environment
        Assert.assertNull("BundleRevision removed", env.getResourceById(resid));
        Assert.assertNull("BundleWiring null", brev.getWiring());
    }

    private String performCall(String context, String pattern, String param) throws Exception {
        String urlspec = managementClient.getWebUri() + "/" + context + "/";
        URL url = new URL(urlspec + pattern + (param != null ? "?input=" + param : ""));
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    @Deployment(name = BUNDLE_V200_WAB, managed = false, testable = false)
    public static Archive<?> getWebV200Wab() {
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
}

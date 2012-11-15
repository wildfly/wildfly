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

import static org.osgi.framework.Constants.ACTIVATION_LAZY;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.integration.osgi.deployment.bundle.AttachedType;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test bundle deployment using the {@link ModelControllerClient}
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Mar-2012
 */
@RunWith(Arquillian.class)
public class ServerDeploymentTestCase {

    static final String GOOD_BUNDLE = "good-bundle.jar";
    static final String GOOD_FRAGMENT = "good-fragment.jar";
    static final String GOOD_FRAGMENT_EAR = "good-fragment.ear";
    static final String BAD_BUNDLE_VERSION = "bad-bundle-version";
    static final String ACTIVATE_LAZILY = "activate-lazily";

    @ArquillianResource
    public Deployer deployer;

    @ArquillianResource
    StartLevel startLevel;

    @ArquillianResource
    PackageAdmin packageAdmin;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundle");
        archive.addClasses(ServerDeploymentHelper.class, FrameworkUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ClientConstants.class, ModelControllerClient.class, DeploymentPlanBuilder.class);
                builder.addImportPackages(PackageAdmin.class, StartLevel.class, ServiceTracker.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testAutoStart() throws Exception {
        ModelControllerClient client = FrameworkUtils.waitForService(context, ModelControllerClient.class);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);

        // Deploy the bundle
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        String runtimeName = server.deploy("auto-start", input);

        // Find the deployed bundle
        Bundle bundle = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testBadBundleVersion() throws Exception {
        ModelControllerClient client = FrameworkUtils.waitForService(context, ModelControllerClient.class);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);
        InputStream input = deployer.getDeployment(BAD_BUNDLE_VERSION);
        try {
            server.deploy(BAD_BUNDLE_VERSION, input);
            Assert.fail("Deployment exception expected");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testRedeployAfterUndeploy() throws Exception {
        ModelControllerClient client = FrameworkUtils.waitForService(context, ModelControllerClient.class);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);

        // Deploy the bundle
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        String runtimeName = server.deploy("redeploy", input);

        // Find the deployed bundle
        Bundle bundle = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());

        // Redeploy the same bundle
        input = deployer.getDeployment(GOOD_BUNDLE);
        runtimeName = server.deploy("redeploy", input);

        // Find the deployed bundle
        bundle = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testAttachedFragment() throws Exception {
        ModelControllerClient client = FrameworkUtils.waitForService(context, ModelControllerClient.class);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);

        // Deploy the fragment
        InputStream input = deployer.getDeployment(GOOD_FRAGMENT);
        String fragmentName = server.deploy("bundle-fragment-attached", input);

        // Find the deployed bundle
        Bundle fragment = packageAdmin.getBundles(GOOD_FRAGMENT, null)[0];
        Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, fragment.getState());

        // Deploy the bundle
        input = deployer.getDeployment(GOOD_BUNDLE);
        String hostName = server.deploy("bundle-host-attached", input);

        // Find the deployed bundle
        Bundle host = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, host.getState());
        Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, fragment.getState());

        Class<?> clazz = host.loadClass("org.jboss.as.test.integration.osgi.deployment.bundle.AttachedType");
        Assert.assertNotNull("Class not null", clazz);
        Assert.assertSame(host, ((BundleReference)clazz.getClassLoader()).getBundle());

        server.undeploy(fragmentName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, fragment.getState());

        server.undeploy(hostName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, host.getState());
    }

    @Test
    public void testUnattachedFragment() throws Exception {
        ModelControllerClient client = FrameworkUtils.waitForService(context, ModelControllerClient.class);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);

        // Deploy the bundle
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        String hostName = server.deploy("bundle-host", input);

        // Find the deployed bundle
        Bundle host = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, host.getState());

        // Deploy the fragment
        input = deployer.getDeployment(GOOD_FRAGMENT);
        String fragmentName = server.deploy("bundle-fragment", input);

        // Find the deployed bundle
        Bundle fragment = packageAdmin.getBundles(GOOD_FRAGMENT, null)[0];
        Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, fragment.getState());

        try {
            host.loadClass("org.jboss.as.test.integration.osgi.deployment.bundle.AttachedType");
            Assert.fail("ClassNotFoundException expected");
        } catch (ClassNotFoundException ex) {
            // expected
        }

        server.undeploy(fragmentName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, fragment.getState());

        server.undeploy(hostName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, host.getState());
    }

    @Test
    public void testAttachedFragmentEar() throws Exception {
        ModelControllerClient client = FrameworkUtils.waitForService(context, ModelControllerClient.class);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);

        // Deploy the fragment
        InputStream input = deployer.getDeployment(GOOD_FRAGMENT_EAR);
        String earName = server.deploy(GOOD_FRAGMENT_EAR, input);

        // Find the deployed fragment
        Bundle fragment = packageAdmin.getBundles(GOOD_FRAGMENT, null)[0];
        Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, fragment.getState());

        // Find the deployed bundle
        Bundle host = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, host.getState());

        Class<?> clazz = host.loadClass("org.jboss.as.test.integration.osgi.deployment.bundle.AttachedType");
        Assert.assertNotNull("Class not null", clazz);
        Assert.assertSame(host, ((BundleReference)clazz.getClassLoader()).getBundle());

        server.undeploy(earName);
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, fragment.getState());
        Assert.assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, host.getState());
    }

    @Deployment(name = GOOD_BUNDLE, managed = false, testable = false)
    public static JavaArchive getGoodBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, GOOD_BUNDLE);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = GOOD_FRAGMENT, managed = false, testable = false)
    public static JavaArchive getGoodFragmentArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, GOOD_FRAGMENT);
        archive.addClasses(AttachedType.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addFragmentHost(GOOD_BUNDLE);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = GOOD_FRAGMENT_EAR, managed = false, testable = false)
    public static EnterpriseArchive getGoodFragmentEar() {
        final EnterpriseArchive archive = ShrinkWrap.create(EnterpriseArchive.class, GOOD_FRAGMENT_EAR);
        archive.add(getGoodBundleArchive(), "/", ZipExporter.class);
        archive.add(getGoodFragmentArchive(), "/", ZipExporter.class);
        return archive;
    }

    @Deployment(name = BAD_BUNDLE_VERSION, managed = false, testable = false)
    public static JavaArchive getBadBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BAD_BUNDLE_VERSION);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader(Constants.BUNDLE_MANIFESTVERSION, "2");
                builder.addManifestHeader(Constants.BUNDLE_SYMBOLICNAME, archive.getName());
                builder.addManifestHeader(BUNDLE_VERSION, "bogus");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = ACTIVATE_LAZILY, managed = false, testable = false)
    public static JavaArchive getLazyActivationArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, ACTIVATE_LAZILY);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivationPolicy(ACTIVATION_LAZY);
                return builder.openStream();
            }
        });
        return archive;
    }
}

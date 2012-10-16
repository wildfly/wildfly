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

import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.as.test.integration.osgi.simple.bundleA.DeployInStartActivator;
import org.jboss.as.test.integration.osgi.simple.bundleA.FailInStartActivator;
import org.jboss.as.test.integration.osgi.simple.bundleA.FailInStopActivator;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Resource;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.Repository;
import org.osgi.service.startlevel.StartLevel;

/**
 * Test bundle deployment using the {@link ModelControllerClient}
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Mar-2012
 */
@RunWith(Arquillian.class)
public class ServerDeploymentTestCase {

    static final String GOOD_BUNDLE = "good-bundle";
    static final String BAD_BUNDLE_VERSION = "bad-bundle-version";
    static final String FAIL_IN_START = "fail-in-start";
    static final String FAIL_IN_STOP = "fail-in-stop";
    static final String DEPLOY_IN_START = "deploy-in-start";
    static final String ACTIVATE_LAZILY = "activate-lazily";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public StartLevel startLevel;

    @Inject
    public PackageAdmin packageAdmin;

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "server-deployment-tests");
        archive.addClasses(ServerDeploymentHelper.class, FrameworkUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ClientConstants.class, ModelControllerClient.class, DeploymentPlanBuilder.class);
                builder.addImportPackages(PackageAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testAutoStart() throws Exception {

        // Deploy the bundle
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        String runtimeName = server.deploy("auto-start", input);

        // Find the deployed bundle
        Bundle bundle = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testDeferredStart() throws Exception {

        // Deploy the bundle
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        String runtimeName = server.deploy("deferred-start", input, false);

        // Find the deployed bundle
        Bundle bundle = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
        assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

        server.undeploy(runtimeName);
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testBundleStartLevel() throws Exception {

        ModelControllerClient client = getModelControllerClient();
        assertNotNull("ModelControllerClient available", client);

        // Setup the deployment metadata
        Map<String, Object> userdata = new HashMap<String, Object>();
        userdata.put(DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL, Integer.valueOf(20));

        // Deploy the bundle
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);
        String runtimeName = server.deploy(GOOD_BUNDLE, input, userdata);
        try {
            // Find the deployed bundle
            Bundle bundle = packageAdmin.getBundles(GOOD_BUNDLE, null)[0];
            assertNotNull("Bundle installed", bundle);

            // Verify that the bundle got installed in @ the specified start level
            int bundleStartLevel = startLevel.getBundleStartLevel(bundle);
            assertEquals("Bundle @ given level", 20, bundleStartLevel);
        } finally {
            server.undeploy(runtimeName);
        }
    }

    @Test
    public void testBadBundleVersion() throws Exception {
        InputStream input = deployer.getDeployment(BAD_BUNDLE_VERSION);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        try {
            server.deploy(BAD_BUNDLE_VERSION, input);
            fail("Deployment exception expected");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testFailInStart() throws Exception {
        InputStream input = deployer.getDeployment(FAIL_IN_START);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        try {
            server.deploy(FAIL_IN_START, input);
            fail("Deployment exception expected");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testFailInStop() throws Exception {
        InputStream input = deployer.getDeployment(FAIL_IN_STOP);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        String runtimeName = server.deploy(FAIL_IN_STOP, input);

        Bundle bundle = packageAdmin.getBundles(FAIL_IN_STOP, null)[0];
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    @Ignore("[AS7-2777] Deadlock on management op from within activate service")
    public void testDeployInStart() throws Exception {

        // Deploy the bundle
        InputStream input = deployer.getDeployment(DEPLOY_IN_START);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        String runtimeName = server.deploy(DEPLOY_IN_START, input);

        // Find the deployed bundle
        Bundle bundle = packageAdmin.getBundles(DEPLOY_IN_START, null)[0];
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testAutoStartWithLazyActivation() throws Exception {

        // Deploy the bundle
        InputStream input = deployer.getDeployment(ACTIVATE_LAZILY);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        String runtimeName = server.deploy(ACTIVATE_LAZILY, input);

        // Find the deployed bundle
        Bundle bundle = packageAdmin.getBundles(ACTIVATE_LAZILY, null)[0];
        assertEquals("Bundle STARTING", Bundle.STARTING, bundle.getState());

        // [TODO] A lazily started bundle cannot be started explicitly through the management interface
        // because the ACTIVATE phase has already been executed. As a workaround stop/start should work
        // server.start(runtimeName);
        // assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        server.undeploy(runtimeName);
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }

    private ModelControllerClient getModelControllerClient() {
        ServiceReference sref = context.getServiceReference(ModelControllerClient.class.getName());
        return (ModelControllerClient) context.getService(sref);
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

    @Deployment(name = FAIL_IN_START, managed = false, testable = false)
    public static JavaArchive getFailInStartArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, FAIL_IN_START);
        archive.addClasses(FailInStartActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivator(FailInStartActivator.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = FAIL_IN_STOP, managed = false, testable = false)
    public static JavaArchive getFailInStopArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, FAIL_IN_STOP);
        archive.addClasses(FailInStopActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivator(FailInStopActivator.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = DEPLOY_IN_START, managed = false, testable = false)
    public static JavaArchive getDeployInStartArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, DEPLOY_IN_START);
        archive.addClasses(DeployInStartActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivator(DeployInStartActivator.class);
                builder.addImportPackages(BundleActivator.class, Resource.class, Repository.class);
                builder.addImportPackages(MavenCoordinates.class);
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
                builder.addBundleActivationPolicy(Constants.ACTIVATION_LAZY);
                return builder.openStream();
            }
        });
        return archive;
    }
}

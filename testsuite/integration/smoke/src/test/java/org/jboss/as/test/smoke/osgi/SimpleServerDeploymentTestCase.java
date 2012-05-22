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
package org.jboss.as.test.smoke.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.ACTIVATION_LAZY;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.osgi.OSGiFrameworkUtils;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * Test bundle deployment using the {@link ModelControllerClient}
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Mar-2012
 */
@RunWith(Arquillian.class)
public class SimpleServerDeploymentTestCase {

    static final String GOOD_BUNDLE = "good-bundle";
    static final String BAD_BUNDLE_VERSION = "bad-bundle-version";
    static final String ACTIVATE_LAZILY = "activate-lazily";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public StartLevel startLevel;

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundle");
        archive.addClasses(ServerDeploymentHelper.class, OSGiFrameworkUtils.class);
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
        Bundle bundle = OSGiFrameworkUtils.getDeployedBundle(context, GOOD_BUNDLE, null);
        assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

        server.undeploy(runtimeName);
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
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

    private ModelControllerClient getModelControllerClient() {
        ServiceReference sref = context.getServiceReference(ModelControllerClient.class.getName());
        return (ModelControllerClient) context.getService(sref);
    }

    @Deployment(name = GOOD_BUNDLE, managed = false, testable = false)
    public static JavaArchive getGoodBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, GOOD_BUNDLE);
        archive.setManifest(new Asset() {
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
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
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

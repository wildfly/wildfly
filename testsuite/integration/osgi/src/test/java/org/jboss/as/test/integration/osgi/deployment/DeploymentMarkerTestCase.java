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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.DeploymentMarker;
import org.jboss.as.test.integration.osgi.deployment.bundle.DeploymentMarkerActivatorB;
import org.jboss.as.test.integration.osgi.deployment.bundle.DeploymentMarkerActivatorC;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * Test bundle deployment using the {@link DeploymentMarker}
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Oct-2012
 */
@RunWith(Arquillian.class)
@Ignore("AS7-6647")
public class DeploymentMarkerTestCase {

    static final String BUNDLE_A = "deploymentmarker-bundle-a";
    static final String BUNDLE_B = "deploymentmarker-bundle-b";
    static final String BUNDLE_C = "deploymentmarker-bundle-c";

    @ArquillianResource
    public Deployer deployer;

    @ArquillianResource
    StartLevel startLevel;

    @ArquillianResource
    PackageAdmin packageAdmin;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deploymentmarker-tests");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PackageAdmin.class, StartLevel.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testAutoStart() throws Exception {
        deployer.deploy(BUNDLE_A);
        try {
            Bundle bundle = packageAdmin.getBundles(BUNDLE_A, null)[0];
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            deployer.undeploy(BUNDLE_A);
        }
    }

    @Test
    public void testAutoStartDisabled() throws Exception {
        deployer.deploy(BUNDLE_B);
        try {
            Bundle bundle = packageAdmin.getBundles(BUNDLE_B, null)[0];
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            deployer.undeploy(BUNDLE_B);
        }
    }

    @Test
    public void testStartLevel() throws Exception {
        deployer.deploy(BUNDLE_C);
        try {
            Bundle bundle = packageAdmin.getBundles(BUNDLE_C, null)[0];
            Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
            bundle.start();
            Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
            Assert.assertEquals(2, startLevel.getBundleStartLevel(bundle));
        } finally {
            deployer.undeploy(BUNDLE_C);
        }
    }

    @Deployment(name = BUNDLE_A, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A);
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

    @Deployment(name = BUNDLE_B, managed = false, testable = false)
    public static JavaArchive getBundleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B);
        archive.addClasses(DeploymentMarkerActivatorB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivator(DeploymentMarkerActivatorB.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_C, managed = false, testable = false)
    public static JavaArchive getBundleC() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_C);
        archive.addClasses(DeploymentMarkerActivatorC.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivator(DeploymentMarkerActivatorC.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

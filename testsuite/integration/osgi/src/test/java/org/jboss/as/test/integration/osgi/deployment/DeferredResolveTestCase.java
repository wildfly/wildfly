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
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.osgi.deployment.bundle.DeferredFailActivator;
import org.jboss.as.test.osgi.FrameworkManagement;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * Test deferred bundle resolution
 *
 * @author thomas.diesler@jboss.com
 * @since 25-Sep-2012
 */
@RunWith(Arquillian.class)
public class DeferredResolveTestCase {

    private static final String GOOD_BUNDLE = "good-bundle.jar";
    private static final String BAD_BUNDLE = "bad-bundle.jar";
    private static final String DEFERRED_BUNDLE_A = "deferred-bundle-a.jar";
    private static final String DEFERRED_BUNDLE_B = "deferred-bundle-b.jar";
    private static final String SIMPLE_AGGREGATE = "simple-aggregate.ear";
    private static final String DEFERRED_AGGREGATE_A = "deferred-aggregate-a.ear";
    private static final String DEFERRED_AGGREGATE_B = "deferred-aggregate-b.ear";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    BundleContext context;

    @ArquillianResource
    StartLevel startLevel;

    @ArquillianResource
    PackageAdmin packageAdmin;

    @Deployment
    public static Archive<?> getDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deferred-resolve-tests");
        archive.addClasses(FrameworkUtils.class, FrameworkManagement.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ModelControllerClient.class, ModelNode.class, ManagementClient.class);
                builder.addImportPackages(PackageAdmin.class, StartLevel.class);
                builder.addImportPackages(XBundle.class, BundleManager.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testResolveAPICall() throws Exception {
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        Bundle bundle = context.installBundle(GOOD_BUNDLE, input);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testResolveStartLevel() throws Exception {
        int orglevel = startLevel.getStartLevel();
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        Bundle bundle = context.installBundle(GOOD_BUNDLE, input);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            startLevel.setBundleStartLevel(bundle, 2);
            bundle.start();
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            FrameworkUtils.changeStartLevel(context, 2, 5, TimeUnit.SECONDS);
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            startLevel.setStartLevel(orglevel);
            bundle.uninstall();
        }
    }

    @Test
    public void testResolveManagementOp() throws Exception {
        InputStream input = deployer.getDeployment(GOOD_BUNDLE);
        Bundle bundle = context.installBundle(GOOD_BUNDLE, input);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            FrameworkManagement.bundleStart(managementClient.getControllerClient(), bundle.getBundleId());
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testFailOnDeploy() throws Exception {
        try {
            deployer.deploy(BAD_BUNDLE);
            Assert.fail("RuntimeException expected");
        } catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testFailAPICall() throws Exception {
        InputStream input = deployer.getDeployment(BAD_BUNDLE);
        Bundle bundle = context.installBundle(BAD_BUNDLE, input);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            try {
                bundle.start();
                Assert.fail("BundleException expected");
            } catch (BundleException e) {
                // expected
            }
            Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

            // Attempt restarting after failure
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testFailStartLevel() throws Exception {
        int orglevel = startLevel.getStartLevel();
        InputStream input = deployer.getDeployment(BAD_BUNDLE);
        Bundle bundle = context.installBundle(BAD_BUNDLE, input);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            startLevel.setBundleStartLevel(bundle, 2);
            bundle.start();
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            FrameworkUtils.changeStartLevel(context, 2, 5, TimeUnit.SECONDS);
            Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

            // Attempt restarting after failure
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            startLevel.setStartLevel(orglevel);
            bundle.uninstall();
        }
    }

    @Test
    public void testFailManagementOp() throws Exception {
        InputStream input = deployer.getDeployment(BAD_BUNDLE);
        Bundle bundle = context.installBundle(BAD_BUNDLE, input);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());
            try {
                FrameworkManagement.bundleStart(managementClient.getControllerClient(), bundle.getBundleId());
                Assert.fail("RuntimeException expected");
            } catch (RuntimeException e) {
                // expected
            }
            Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

            // Attempt restarting after failure
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testUnresolvedRequirement() throws Exception {
        deployer.deploy(DEFERRED_BUNDLE_A);
        try {
            Bundle bundleA = packageAdmin.getBundles(DEFERRED_BUNDLE_A, null)[0];
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
            deployer.deploy(DEFERRED_BUNDLE_B);
            try {
                Bundle bundleB = packageAdmin.getBundles(DEFERRED_BUNDLE_B, null)[0];
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());

                packageAdmin.resolveBundles(new Bundle[] { bundleA });

                Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundleA.getState());
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());

                bundleA.start();
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
            } finally {
                deployer.undeploy(DEFERRED_BUNDLE_B);
            }
        } finally {
            deployer.undeploy(DEFERRED_BUNDLE_A);
        }
    }

    @Test
    public void testSimpleAggregate() throws Exception {
        deployer.deploy(SIMPLE_AGGREGATE);
        try {
            Bundle bundleA = packageAdmin.getBundles(DEFERRED_BUNDLE_A, null)[0];
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
            Bundle bundleB = packageAdmin.getBundles(DEFERRED_BUNDLE_B, null)[0];
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());
        } finally {
            deployer.undeploy(SIMPLE_AGGREGATE);
        }
    }

    @Test
    public void testAggregateWithDeferredModule() throws Exception {
        deployer.deploy(DEFERRED_AGGREGATE_A);
        try {
            Bundle bundleA = packageAdmin.getBundles(DEFERRED_BUNDLE_A, null)[0];
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
            deployer.deploy(DEFERRED_BUNDLE_B);
            try {
                Bundle bundleB = packageAdmin.getBundles(DEFERRED_BUNDLE_B, null)[0];
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());

                packageAdmin.resolveBundles(new Bundle[] { bundleA });

                Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundleA.getState());
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());

                bundleA.start();
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
            } finally {
                deployer.undeploy(DEFERRED_BUNDLE_B);
            }
        } finally {
            deployer.undeploy(DEFERRED_AGGREGATE_A);
        }
    }

    @Test
    public void testAggregateWithUndeferredModule() throws Exception {
        deployer.deploy(DEFERRED_AGGREGATE_B);
        try {
            Bundle bundleA = packageAdmin.getBundles(DEFERRED_BUNDLE_A, null)[0];
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
            deployer.deploy(DEFERRED_BUNDLE_B);
            try {
                Bundle bundleB = packageAdmin.getBundles(DEFERRED_BUNDLE_B, null)[0];
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());

                packageAdmin.resolveBundles(new Bundle[] { bundleA });

                Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundleA.getState());
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());

                bundleA.start();
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
            } finally {
                deployer.undeploy(DEFERRED_BUNDLE_B);
            }
        } finally {
            deployer.undeploy(DEFERRED_AGGREGATE_B);
        }
    }

    @Deployment(name = GOOD_BUNDLE, managed = false, testable = false)
    public static Archive<?> getGoodBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, GOOD_BUNDLE);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BAD_BUNDLE, managed = false, testable = false)
    public static Archive<?> getBadBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BAD_BUNDLE);
        archive.addClasses(DeferredFailActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(DeferredFailActivator.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = DEFERRED_BUNDLE_A, managed = false, testable = false)
    public static Archive<?> getDeferredA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, DEFERRED_BUNDLE_A);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages("org.acme.foo");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = DEFERRED_BUNDLE_B, managed = false, testable = false)
    public static Archive<?> getDeferredB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, DEFERRED_BUNDLE_B);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages("org.acme.foo");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = SIMPLE_AGGREGATE, managed = false, testable = false)
    public static Archive<?> getSimpleAggregate() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_AGGREGATE);
        ear.addAsModule(getDeferredA());
        ear.addAsModule(getDeferredB());
        return ear;
    }

    @Deployment(name = DEFERRED_AGGREGATE_A, managed = false, testable = false)
    public static Archive<?> getDeferredAggregateA() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DEFERRED_AGGREGATE_A);
        ear.addAsModule(getDeferredA());
        return ear;
    }

    @Deployment(name = DEFERRED_AGGREGATE_B, managed = false, testable = false)
    public static Archive<?> getDeferredAggregateB() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DEFERRED_AGGREGATE_B);
        ear.addAsModule(getGoodBundle());
        ear.addAsModule(getDeferredA());
        return ear;
    }
}

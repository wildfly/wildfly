/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.osgi.classloading;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.classloading.suba.TestA;
import org.jboss.as.test.integration.osgi.classloading.subb.TestBA;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test optional imports
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Aug-2011
 */
@RunWith(Arquillian.class)
public class OptionalImportTestCase {

    static final String BUNDLE_A = "optional-import-a";
    static final String BUNDLE_B = "optional-import-b";

    @ArquillianResource
    Deployer deployer;

    @Inject
    public BundleContext context;

    @Inject
    public PackageAdmin packageAdmin;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "optional-import-tests");
        archive.addClasses(TestBA.class);
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

    @Test
    public void testUnresolvedOptionalImport() throws Exception {
        deployer.deploy(BUNDLE_A);
        Bundle bundleA = packageAdmin.getBundles(BUNDLE_A, null)[0];
        try {
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
            try {
                bundleA.loadClass(TestBA.class.getName()).newInstance();
                Assert.fail("NoClassDefFoundError expected");
            } catch (NoClassDefFoundError ex) {
                // expected
            }
        } finally {
            deployer.undeploy(BUNDLE_A);
        }
    }

    @Test
    public void testResolvedOptionalImport() throws Exception {
        deployer.deploy(BUNDLE_B);
        Bundle bundleB = packageAdmin.getBundles(BUNDLE_B, null)[0];
        try {
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleB.getState());
            deployer.deploy(BUNDLE_A);
            Bundle bundleA = packageAdmin.getBundles(BUNDLE_A, null)[0];
            try {
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
                bundleA.loadClass(TestBA.class.getName()).newInstance();
            } finally {
                deployer.undeploy(BUNDLE_A);
            }
        } finally {
            deployer.undeploy(BUNDLE_B);
        }
    }

    @Test
    public void testUnresolvedOptionalImportAPI() throws Exception {
        InputStream inputA = deployer.getDeployment(BUNDLE_A);
        Bundle bundleA = context.installBundle(BUNDLE_A, inputA);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
            bundleA.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
            try {
                bundleA.loadClass(TestBA.class.getName()).newInstance();
                Assert.fail("NoClassDefFoundError expected");
            } catch (NoClassDefFoundError ex) {
                // expected
            }
        } finally {
            bundleA.uninstall();
        }
    }

    @Test
    public void testResolvedOptionalImportAPI() throws Exception {
        InputStream inputB = deployer.getDeployment(BUNDLE_B);
        Bundle bundleB = context.installBundle(BUNDLE_B, inputB);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleB.getState());
            InputStream inputA = deployer.getDeployment(BUNDLE_A);
            Bundle bundleA = context.installBundle(BUNDLE_A, inputA);
            try {
                Assert.assertEquals(Bundle.INSTALLED, bundleA.getState());
                bundleA.start();
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleA.getState());
                Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundleB.getState());
                bundleA.loadClass(TestBA.class.getName()).newInstance();
            } finally {
                bundleA.uninstall();
            }
        } finally {
            bundleB.uninstall();
        }
    }

    @Deployment(name = BUNDLE_A, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A);
        archive.addClasses(TestBA.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(TestA.class.getPackage().getName() + ";resolution:=optional");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_B, managed = false, testable = false)
    public static JavaArchive getBundleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B);
        archive.addClasses(TestA.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages(TestA.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

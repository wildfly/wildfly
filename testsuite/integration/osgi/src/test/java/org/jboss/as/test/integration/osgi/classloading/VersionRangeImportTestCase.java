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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Test imports with version range
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Aug-2011
 */
@RunWith(Arquillian.class)
public class VersionRangeImportTestCase {

    static final String BUNDLE_A = "version-range-a";
    static final String BUNDLE_B = "version-range-b";
    static final String BUNDLE_C = "version-range-c";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "version-range-tests");
        return archive;
    }

    @Test
    public void testVersionNotInRange() throws Exception {
        InputStream inputA = deployer.getDeployment(BUNDLE_A);
        Bundle bundleA = context.installBundle(BUNDLE_A, inputA);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
            InputStream inputB = deployer.getDeployment(BUNDLE_B);
            Bundle bundleB = context.installBundle(BUNDLE_B, inputB);
            try {
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleB.getState());
                try {
                    bundleB.start();
                    Assert.fail("BundleException expected");
                } catch (BundleException ex) {
                    // expected
                }
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleB.getState());
            } finally {
                bundleB.uninstall();
            }
        } finally {
            bundleA.uninstall();
        }
    }

    @Test
    public void testVersionInRange() throws Exception {
        InputStream inputA = deployer.getDeployment(BUNDLE_A);
        Bundle bundleA = context.installBundle(BUNDLE_A, inputA);
        try {
            Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleA.getState());
            InputStream inputC = deployer.getDeployment(BUNDLE_C);
            Bundle bundleC = context.installBundle(BUNDLE_C, inputC);
            try {
                Assert.assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundleC.getState());
                bundleC.start();
                Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundleC.getState());
                Assert.assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundleA.getState());
            } finally {
                bundleC.uninstall();
            }
        } finally {
            bundleA.uninstall();
        }
    }

    @Deployment(name = BUNDLE_A, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages("org.acme.foo;version=2.0.0");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_B, managed = false, testable = false)
    public static JavaArchive getBundleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages("org.acme.foo;version='[1.0,2.0)'");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_C, managed = false, testable = false)
    public static JavaArchive getBundleC() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_C);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages("org.acme.foo;version='[2.0,3.0)'");
                return builder.openStream();
            }
        });
        return archive;
    }
}

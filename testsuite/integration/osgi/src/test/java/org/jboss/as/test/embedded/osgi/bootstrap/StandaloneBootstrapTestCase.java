/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.as.test.embedded.osgi.bootstrap;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.osgi.launcher.EmbeddedFrameworkFactory;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.vfs.VFSUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Tests {@link FrameworkFactory}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Dec-2012
 */
@org.junit.Ignore("AS7-6511")
public class StandaloneBootstrapTestCase {

    private static final String BUNDLE_A = "bundleA";
    private static final String BUNDLE_B = "bundleB";
    private static final String FRAGMENT_A = "fragmentA";

    @Before
    public void setUp () {
        File datadir = new File(System.getProperty("jboss.server.data.dir"));
        VFSUtils.recursiveDelete(datadir);
    }

    @Test
    public void testRestartTwoHostsOneFragment() throws Exception {
        FrameworkFactory factory = new EmbeddedFrameworkFactory();
        Framework framework = factory.newFramework(getFrameworkProperties());
        try {
            framework.start();
            BundleContext syscontext = framework.getBundleContext();

            Bundle hostA = installBundle(syscontext, getHostA());
            Bundle fragA = installBundle(syscontext, getFragmentA());
            Bundle hostB = installBundle(syscontext, getHostB());

            hostB.start();

            Assert.assertEquals(Bundle.INSTALLED, hostA.getState());
            Assert.assertEquals(Bundle.INSTALLED, fragA.getState());
            Assert.assertEquals(Bundle.ACTIVE, hostB.getState());

            framework.stop();
            framework.waitForStop(5000);
            framework.start();

            syscontext = framework.getBundleContext();
            PackageAdmin packageAdmin = getPackageAdmin(syscontext);
            hostA = packageAdmin.getBundles(BUNDLE_A, null)[0];
            fragA = packageAdmin.getBundles(FRAGMENT_A, null)[0];
            hostB = packageAdmin.getBundles(BUNDLE_B, null)[0];

            Assert.assertEquals(Bundle.INSTALLED, hostA.getState());
            Assert.assertEquals(Bundle.INSTALLED, fragA.getState());
            //Assert.assertEquals(Bundle.ACTIVE, hostB.getState());

            hostB.uninstall();
            fragA.uninstall();
            hostA.uninstall();

        } finally {
            framework.stop();
            framework.waitForStop(5000);
        }
    }

    private Map<String, String> getFrameworkProperties() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.osgi.service.packageadmin");
        //props.put(Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT, Constants.FRAMEWORK_STORAGE_CLEAN);
        return props;
    }

    private PackageAdmin getPackageAdmin(BundleContext syscontext) {
        ServiceReference sref = syscontext.getServiceReference(PackageAdmin.class.getName());
        return (PackageAdmin) syscontext.getService(sref);
    }

    private Bundle installBundle(BundleContext context, JavaArchive archive) throws BundleException {
        InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
        return context.installBundle(archive.getName(), input);
    }

    private static JavaArchive getHostA() {
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

    private static JavaArchive getFragmentA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, FRAGMENT_A);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addFragmentHost(BUNDLE_A);
                return builder.openStream();
            }
        });
        return archive;
    }

    private static JavaArchive getHostB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B);
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
}

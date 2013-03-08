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
package org.jboss.as.test.integration.osgi.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test framework/bundle start level
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
@org.junit.Ignore("AS7-6511")
public class StartLevelTestCase {

    private static final String BUNDLE_A = "osgi-startlevel-a";

    static final int TIMEOUT = 6000;

    @ArquillianResource
    public Deployer deployer;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "startlevel-testcase");
        archive.addClass(FrameworkUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ServiceTracker.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testStartLevel() throws Exception {

        int orglevel = FrameworkUtils.getFrameworkStartLevel(context);
        assertEquals("Framework start level", 1, orglevel);

        Bundle sysbundle = context.getBundle();
        FrameworkStartLevel fwStartLevel = sysbundle.adapt(FrameworkStartLevel.class);
        int initialStartLevel = fwStartLevel.getInitialBundleStartLevel();
        assertEquals("Initial bundle start level", 1, initialStartLevel);

        InputStream input = deployer.getDeployment(BUNDLE_A);
        Bundle bundle = context.installBundle(BUNDLE_A, input);
        try {
            assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

            BundleStartLevel bStartLevel = bundle.adapt(BundleStartLevel.class);
            bStartLevel.setStartLevel(3);
            assertEquals("Bundle start level", 3, bStartLevel.getStartLevel());

            // Change the framework start level and wait for the changed event
            FrameworkUtils.setFrameworkStartLevel(context, 2);
            assertEquals("Framework start level", 2, fwStartLevel.getStartLevel());

            try {
                bundle.start(Bundle.START_TRANSIENT);
                fail("Bundle cannot be started due to the Framework's current start level");
            } catch (BundleException ex) {
                // expected
            }
            assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

            // The bundle should not be started
            bundle.start();
            assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

            // Change the framework start level and wait for the changed event
            FrameworkUtils.setFrameworkStartLevel(context, 3);

            // The bundle should now be started
            assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        } finally {
            bundle.uninstall();
            FrameworkUtils.setFrameworkStartLevel(context, orglevel);
        }
    }

    @Deployment(name = BUNDLE_A, managed = false, testable=false)
    public static JavaArchive create() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A);
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
}

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
package org.jboss.as.test.smoke.embedded.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.startlevel.StartLevel;

/**
 * [ARQ-465] Add suport for bundle start level
 *
 * https://issues.jboss.org/browse/ARQ-465
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
@Ignore("Ignored after moving to integration tests, other osgi tests interfere with it")
public class SimpleStartLevelTestCase {

    @Inject
    public Bundle bundle;

    @Inject
    public BundleContext context;

    @Inject
    public StartLevel startLevel;

    @Deployment
    @StartLevelAware(startLevel = 3)
    public static JavaArchive create() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "arq465-bundle");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(StartLevel.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testStartLevel() throws Exception {

        assertNotNull("StartLevel injected", startLevel);
        int initialStartLevel = startLevel.getInitialBundleStartLevel();
        assertEquals("Initial bundle start level", 1, initialStartLevel);

        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
        assertEquals("arq465-bundle", bundle.getSymbolicName());

        int bundleStartLevel = startLevel.getBundleStartLevel(bundle);
        assertEquals("Bundle start level", 3, bundleStartLevel);

        try {
            bundle.start(Bundle.START_TRANSIENT);
            fail("Bundle cannot be started due to the Framework's current start level");
        } catch (BundleException ex) {
            // expected
        }
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

        // The bundle should not be started
        bundle.start();
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

        // Change the frameworkj start level and wait for the changed event
        final CountDownLatch latch = new CountDownLatch(1);
        context.addFrameworkListener(new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED)
                    latch.countDown();
            }
        });
        startLevel.setStartLevel(3);
        if(!latch.await(6, TimeUnit.SECONDS)) {
            Assert.fail("Bundle did not start within 6 seconds");
        }

        // The bundle should now be started
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        bundle.stop();
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

        bundle.uninstall();
        assertEquals("Bundle UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());
    }
}

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
package org.jboss.as.osgi.launcer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.osgi.launcher.EmbeddedFrameworkFactory;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Tests {@link FrameworkFactory}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Dec-2012
 */
public class FrameworkFactoryTestCase {

    @Test
    public void testFrameworkInit() throws Exception {
        FrameworkFactory factory = new EmbeddedFrameworkFactory();
        Framework framework = factory.newFramework(null);
        try {
            Assert.assertEquals(Bundle.INSTALLED, framework.getState());

            framework.init();
            // [TODO] init activates the subsystem and takes it to active
            // Assert.assertEquals(Bundle.STARTING, framework.getState());

            // It should be possible to install a bundle into this framework, even though it's only inited...
            BundleContext syscontext = framework.getBundleContext();
            Bundle bundle = installBundle(syscontext, getBundleA());
            Assert.assertEquals(Bundle.INSTALLED, bundle.getState());
            Assert.assertNotNull("BundleContext not null", framework.getBundleContext());

            framework.stop();
            Assert.assertNull("BundleContext null", framework.getBundleContext());
            FrameworkEvent stopEvent = framework.waitForStop(2000);
            Assert.assertEquals(FrameworkEvent.STOPPED, stopEvent.getType());
        } finally {
            framework.stop();
            framework.waitForStop(5000);
        }
    }

    //@Test
    public void testFrameworkStartStop() throws Exception {

        Map<String, Object> props = getFrameworkInitProperties(true);
        FrameworkFactory factory = new EmbeddedFrameworkFactory();
        Framework framework = factory.newFramework(props);
        try {
            Assert.assertNotNull("Framework not null", framework);
            Assert.assertEquals(Bundle.INSTALLED, framework.getState());

            framework.init();
            // [TODO] init activates the subsystem and takes it to active
            // Assert.assertEquals(Bundle.STARTING, framework.getState());

            BundleContext syscontext = framework.getBundleContext();
            Assert.assertNotNull("BundleContext not null", syscontext);
            Bundle systemBundle = syscontext.getBundle();
            Assert.assertNotNull("Bundle not null", systemBundle);
            Assert.assertEquals("System bundle id", 0, systemBundle.getBundleId());

            framework.start();
            Assert.assertEquals(Bundle.ACTIVE, framework.getState());

            framework.stop();
            FrameworkEvent stopEvent = framework.waitForStop(2000);
            Assert.assertEquals(FrameworkEvent.STOPPED, stopEvent.getType());
            Assert.assertEquals(Bundle.RESOLVED, framework.getState());
        } finally {
            framework.stop();
            framework.waitForStop(5000);
        }
    }

    private Bundle installBundle(BundleContext context, JavaArchive archive) throws BundleException {
        InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
        return context.installBundle(archive.getName(), input);
    }

    private Map<String, Object> getFrameworkInitProperties(boolean cleanOnFirstInit) {
        Map<String, Object> props = new HashMap<String, Object>();
        if (cleanOnFirstInit == true) {
            props.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        }
        return props;
    }

    private static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundleA");
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
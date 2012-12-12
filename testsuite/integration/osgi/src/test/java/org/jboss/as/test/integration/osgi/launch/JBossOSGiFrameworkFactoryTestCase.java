/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.integration.osgi.launch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.osgi.launcher.JBossOSGiFrameworkFactory;
import org.jboss.as.test.integration.osgi.launch.bundle.Activator;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

/**
 * @author David Bosschaert
 */
public class JBossOSGiFrameworkFactoryTestCase extends EmbeddedOSGiFrameworkLauncherTestBase {
    @Test
    public void testFrameworkFactory() throws Exception {
        HashMap<String, String> fwProps = new HashMap<String, String>();
        fwProps.put("foo", "bar");
        Framework framework = new JBossOSGiFrameworkFactory().newFramework(fwProps);
        framework.init();
        Assert.assertEquals(Bundle.STARTING, framework.getState());

        framework.start();
        Assert.assertEquals(Bundle.ACTIVE, framework.getState());

        Assert.assertEquals(0, framework.getBundleId());

        BundleContext sctx = framework.getBundleContext();
        Assert.assertEquals("bar", sctx.getProperty("foo"));

        Bundle b = sctx.installBundle("myBundle1.jar", getTestBundle());
        b.start();

        ServiceReference[] refs = sctx.getServiceReferences(String.class.getName(), "(org.jboss.launch.test=testproperty)");
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("a string service", sctx.getService(refs[0]));

        final List<String> events = Collections.synchronizedList(new ArrayList<String>());
        BundleListener listener = new BundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                events.add(event.getBundle().getSymbolicName() + event.getType());
            }
        };

        framework.getBundleContext().addBundleListener(listener);

        Assert.assertEquals(0, events.size());
        FrameworkEvent event = framework.waitForStop(0);
        Assert.assertEquals(FrameworkEvent.STOPPED, event.getType());

        Assert.assertTrue("Bundle should have been stopped", events.contains(b.getSymbolicName() + BundleEvent.STOPPED));
        Assert.assertEquals(Framework.RESOLVED, framework.getState());
    }

    private InputStream getTestBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundle1.jar");
        archive.addClasses(Activator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleContext.class);
                builder.addBundleActivator(Activator.class);
                return builder.openStream();
            }
        });
        ZipExporter ze = archive.as(ZipExporter.class);
        return ze.exportAsInputStream();
    }
}

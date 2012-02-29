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
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.smoke.osgi.bundle.SimpleService;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test the registration of a non-OSGi deployement.
 *
 * @author thomas.diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class SimpleModuleRegistrationTestCase {

    @Inject
    public BundleContext bundleContext;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-module-reg");
        archive.addClass(SimpleService.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core");
                return builder.openStream();
            }
        });
        archive.addAsManifestResource(new StringAsset(
                Constants.BUNDLE_SYMBOLICNAME + ": " + archive.getName() + "\n" +
                Constants.EXPORT_PACKAGE + ": " + SimpleService.class.getPackage().getName()),
                "jbosgi-xservice.properties");
        return archive;
    }

    @Test
    public void testModuleRegistered() throws Exception {

        assertNotNull("BundleContext injected", bundleContext);

        ServiceReference sref = bundleContext.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin) bundleContext.getService(sref);
        assertNotNull("PackageAdmin not null", pa);

        Bundle[] bundles = pa.getBundles("example-module-reg", null);
        assertNotNull("Bundles not null", bundles);
        assertEquals("One bundle", 1, bundles.length);

        Bundle bundle = bundles[0];
        assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

        Class<?> clazz = bundle.loadClass(SimpleService.class.getName());
        assertNotNull("Loaded class", clazz);
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());

        ExportedPackage[] exportedPackages = pa.getExportedPackages(bundle);
        assertNotNull("ExportedPackages not null", exportedPackages);
        assertEquals("One ExportedPackage", 1, exportedPackages.length);

        ExportedPackage exportedPackage = exportedPackages[0];
        assertEquals(SimpleService.class.getPackage().getName(), exportedPackage.getName());
    }
}

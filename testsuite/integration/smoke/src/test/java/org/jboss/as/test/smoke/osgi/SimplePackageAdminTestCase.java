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
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * [ARQ-466] Add support for injected PackageAdmin
 *
 * https://issues.jboss.org/browse/ARQ-466
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
public class SimplePackageAdminTestCase {

    @Inject
    public Bundle bundle;

    @Inject
    public PackageAdmin packageAdmin;

    @Deployment
    public static JavaArchive create() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "arq466-bundle");
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
    public void testPackageAdmin() throws Exception {

        assertNotNull("PackageAdmin injected", packageAdmin);

        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
        assertEquals("arq466-bundle", bundle.getSymbolicName());

        Bundle[] bundles = packageAdmin.getBundles("arq466-bundle", null);
        assertNotNull("Bundles not null", bundles);
        assertEquals("One bundle found", 1, bundles.length);
        assertEquals(bundle, bundles[0]);
    }
}

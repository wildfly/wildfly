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

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that a non OSGi module can have the system context injected
 *
 * @author thomas.diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class SimpleBundleContextTestCase {

    @Inject
    public BundleContext bundleContext;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundlecontext");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleContextInjection() throws Exception {

        // Assert that the injected bundle
        assertNotNull("BundleContext not injected", bundleContext);

        Bundle bundle = bundleContext.getBundle();
        assertNotNull("Bundle is null", bundle);
        assertEquals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        assertEquals(Version.emptyVersion, bundle.getVersion());
        assertEquals(0, bundle.getBundleId());
    }
}

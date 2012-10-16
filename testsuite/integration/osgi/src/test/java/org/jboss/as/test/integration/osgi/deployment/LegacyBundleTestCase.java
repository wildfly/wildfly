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
package org.jboss.as.test.integration.osgi.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.Constants.BUNDLE_NAME;

import java.io.InputStream;

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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Version;

/**
 * Test simple OSGi bundle deployment
 *
 * @author thomas.diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class LegacyBundleTestCase {

    @ArquillianResource
    Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "legacy-bundle");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleName(archive.getName());
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleInjection() throws Exception {

        // Assert that the injected bundle
        assertNotNull("Bundle injected", bundle);
        assertEquals("legacy-bundle", bundle.getHeaders().get(BUNDLE_NAME));
        assertEquals(Version.emptyVersion, bundle.getVersion());

        // Assert that the bundle is in state ACTIVE
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());

        // Stop the bundle
        bundle.stop();
        Assert.assertEquals(Bundle.RESOLVED, bundle.getState());

        // Start the bundle
        bundle.start();
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
    }
}

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
package org.jboss.as.test.embedded.demos.osgi;

import java.io.InputStream;

import org.jboss.arquillian.api.ArchiveProvider;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.embedded.demos.osgi.bundle.SimpleActivator;
import org.jboss.as.test.embedded.demos.osgi.bundle.SimpleService;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the arquillian callback to a client provided archive
 *
 * @author thomas.diesler@jboss.com
 * @since 09-Sep-2010
 */
@RunWith(Arquillian.class)
public class SimpleArchiveProviderTestCase {

//    @Inject
//    public OSGiContainer container;

    @Test
    public void testBundleInjection() throws Exception {
        throw new IllegalStateException("Test has been commented out due to changes in API");

//        Archive<?> archive = container.getTestArchive("example-archive-provider");
//        Bundle bundle = container.installBundle(archive);
//        try {
//            // Assert that the bundle is in state INSTALLED
//            OSGiTestHelper.assertBundleState(Bundle.INSTALLED, bundle.getState());
//
//            // Start the bundle
//            bundle.start();
//            OSGiTestHelper.assertBundleState(Bundle.ACTIVE, bundle.getState());
//
//            // Stop the bundle
//            bundle.stop();
//            OSGiTestHelper.assertBundleState(Bundle.RESOLVED, bundle.getState());
//        } finally {
//            bundle.uninstall();
//            OSGiTestHelper.assertBundleState(Bundle.UNINSTALLED, bundle.getState());
//        }
    }

    @ArchiveProvider
    public static JavaArchive getTestArchive(String name) {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name);
        archive.addClasses(SimpleActivator.class, SimpleService.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(SimpleActivator.class.getName());
                return builder.openStream();
            }
        });
        return archive;
    }
}

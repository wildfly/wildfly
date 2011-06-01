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
package org.jboss.as.testsuite.integration.osgi.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.osgi.xservice.bundle.SimpleActivator;
import org.jboss.as.testsuite.integration.osgi.xservice.bundle.SimpleService;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A simple OSGi bundle test case
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2011
 */
@RunWith(Arquillian.class)
public class SimpleBundleTestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "simple-bundle");
        archive.addClasses(SimpleActivator.class, SimpleService.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(SimpleActivator.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleInjection() throws Exception {

        // Assert that the bundle is injected
        assertNotNull("Bundle injected", bundle);

        // Assert that the bundle is in state RESOLVED
        // Note when the test bundle contains the test case it
        // must be resolved already when this test method is called
        OSGiTestHelper.assertBundleState(Bundle.RESOLVED, bundle.getState());

        // Start the bundle
        bundle.start();
        OSGiTestHelper.assertBundleState(Bundle.ACTIVE, bundle.getState());

        // Get the service reference
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(SimpleService.class.getName());
        assertNotNull("ServiceReference not null", sref);

        // Get the service for the reference
        SimpleService service = (SimpleService) context.getService(sref);
        assertNotNull("Service not null", service);

        // Invoke the service
        int sum = service.sum(1, 2, 3);
        assertEquals(6, sum);

        // Stop the bundle
        bundle.stop();
        OSGiTestHelper.assertBundleState(Bundle.RESOLVED, bundle.getState());
    }
}

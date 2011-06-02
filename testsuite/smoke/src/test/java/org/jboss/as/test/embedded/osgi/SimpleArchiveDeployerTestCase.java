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
package org.jboss.as.test.embedded.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.embedded.osgi.bundle.SimpleActivator;
import org.jboss.as.test.embedded.osgi.bundle.SimpleService;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test the arquillian callback to a client provided archive and its deployment through the deployer API.
 *
 * @author thomas.diesler@jboss.com
 * @since 09-Sep-2010
 */
@RunWith(Arquillian.class)
@Ignore("[AS7-734] Migrate to ARQ Beta1")
public class SimpleArchiveDeployerTestCase {

    //@Inject
    //public DeploymentProvider provider;

    //@Inject
    //public ArchiveDeployer archiveDeployer;

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-deployment-provider");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                // [TODO] remove these explicit imports
                builder.addImportPackages("org.jboss.shrinkwrap.impl.base.path");
                builder.addImportPackages(PackageAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testClientDeploymentAsArchive() throws Exception {

        String symbolicName = "archive-deployer-test-bundle";
        Archive<?> archive = null; //provider.getClientDeployment(symbolicName);
        String depname = null; //archiveDeployer.deploy(archive);

        final Bundle bundle = getDeployedBundle(symbolicName);
        assertNotNull("Bundle found", bundle);

        // Start the bundle
        bundle.start();
        OSGiTestHelper.assertBundleState(Bundle.ACTIVE, bundle.getState());

        // Stop the bundle
        bundle.stop();
        OSGiTestHelper.assertBundleState(Bundle.RESOLVED, bundle.getState());

        final CountDownLatch uninstallLatch = new CountDownLatch(1);
        context.addBundleListener(new BundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                Bundle eventSource = event.getBundle();
                if (eventSource.equals(bundle) && event.getType() == BundleEvent.UNINSTALLED)
                    uninstallLatch.countDown();
            }
        });

        //archiveDeployer.undeploy(depname);

        if (uninstallLatch.await(1000, TimeUnit.MILLISECONDS) == false)
            fail("UNINSTALLED event not received");

        OSGiTestHelper.assertBundleState(Bundle.UNINSTALLED, bundle.getState());
    }

    private Bundle getDeployedBundle(String symbolicName) {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) context.getService(sref);
        Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
        assertNotNull("Bundles found", bundles);
        assertEquals("One bundle found", 1, bundles.length);
        return bundles[0];
    }

    //@ArchiveProvider
    public static JavaArchive getTestArchive(String name) {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name);
        archive.addClasses(SimpleActivator.class, SimpleService.class);
        archive.setManifest(new Asset() {
            @Override
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
}

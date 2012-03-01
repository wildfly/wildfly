/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.osgi.xservice;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.xservice.api.Echo;
import org.jboss.as.test.integration.osgi.xservice.bundle.TargetBundleActivator;
import org.jboss.as.test.integration.osgi.xservice.module.ClientModuleTwoActivator;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A test that shows how a module can access another module's service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2011
 */
@RunWith(Arquillian.class)
public class ModuleAccessesBundleServiceTestCase extends AbstractXServiceTestCase {

    private static final String TARGET_BUNDLE_NAME = "example-xservice-mab-target-bundle";
    private static final String CLIENT_MODULE_NAME = "example-xservice-mab-client-module";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "xservice-module-access");
        archive.addClasses(AbstractXServiceTestCase.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Logger.class, PackageAdmin.class, Module.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Inject
    public ServiceContainer serviceContainer;

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public BundleContext systemContext;

    @Override
    ServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    @Test
    public void moduleInvokesBundleService() throws Exception {

        // Deploy the bundle which contains the target service
        deployer.deploy(TARGET_BUNDLE_NAME);
        try {
            // Find the installed bundle using PackageAdmin
            ServiceReference sref = systemContext.getServiceReference(PackageAdmin.class.getName());
            PackageAdmin packageAdmin = (PackageAdmin) systemContext.getService(sref);
            Bundle[] bundles = packageAdmin.getBundles(TARGET_BUNDLE_NAME, null);
            assertNotNull("Bundles not null", bundles);
            assertEquals("One Bundle", 1, bundles.length);

            // Verify that the bundle got started automatically
            final Bundle targetBundle = bundles[0];
            if (targetBundle.getState() != Bundle.ACTIVE) {
                final CountDownLatch startedLatch = new CountDownLatch(1);
                systemContext.addBundleListener(new BundleListener() {
                    public void bundleChanged(BundleEvent event) {
                        Bundle auxBundle = event.getBundle();
                        if (targetBundle.equals(auxBundle) && BundleEvent.STARTED == event.getType()) {
                            startedLatch.countDown();
                        }
                    }
                });
                startedLatch.await(5, TimeUnit.SECONDS);
            }
            Assert.assertEquals(Bundle.ACTIVE, targetBundle.getState());

            // Install the client module
            deployer.deploy(CLIENT_MODULE_NAME);
            try {
                // Check that the client service is up
                ServiceName clientService = ServiceName.parse("jboss.osgi.example.invoker.service");
                assertServiceState(clientService, State.UP, 5000);
            } finally {
                // Undeploy the client module
                deployer.undeploy(CLIENT_MODULE_NAME);
            }
        } finally {
            // Undeploy the target bundle
            deployer.undeploy(TARGET_BUNDLE_NAME);
        }
    }

    @Deployment(name = CLIENT_MODULE_NAME, managed = false, testable = false)
    public static JavaArchive getClientModuleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CLIENT_MODULE_NAME);
        archive.addClasses(ClientModuleTwoActivator.class);
        String activatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addAsResource("osgi/xservice/client-module-two/" + activatorPath, activatorPath);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,org.jboss.modules,org.jboss.logging,org.jboss.osgi.framework,deployment.example-xservice-mab-target-bundle:0.0.0");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = TARGET_BUNDLE_NAME, managed = false, testable = false)
    public static JavaArchive getTargetBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, TARGET_BUNDLE_NAME);
        archive.addClasses(Echo.class, TargetBundleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(TargetBundleActivator.class);
                builder.addImportPackages(Logger.class, BundleActivator.class, Module.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

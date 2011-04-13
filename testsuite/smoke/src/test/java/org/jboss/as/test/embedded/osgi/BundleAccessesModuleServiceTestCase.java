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

package org.jboss.as.test.embedded.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.jboss.arquillian.api.ArchiveDeployer;
import org.jboss.arquillian.api.ArchiveProvider;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.DeploymentProvider;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.embedded.osgi.api.Echo;
import org.jboss.as.test.embedded.osgi.bundle.ClientBundleActivator;
import org.jboss.as.test.embedded.osgi.module.EchoService;
import org.jboss.as.test.embedded.osgi.module.TargetModuleActivator;
import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * A test that shows how a bundle can access an MSC service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
@RunWith(Arquillian.class)
public class BundleAccessesModuleServiceTestCase extends AbstractXServiceTestCase {

    private static final String TARGET_MODULE_NAME = "example-xservice-target-module";
    private static final String CLIENT_BUNDLE_NAME = "example-xservice-client-bundle";

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrapUtils.createJavaArchive("osgi/xservice/module-access-test.jar", AbstractXServiceTestCase.class);
    }

    @Inject
    public ServiceContainer serviceContainer;

    @Inject
    public DeploymentProvider deploymentProvider;

    @Inject
    public ArchiveDeployer archiveDeployer;

    @Inject
    public BundleContext context;

    @Override
    ServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    @Test
    public void bundleInvokesModuleService() throws Exception {
        // Deploy the non-OSGi module which contains the target service
        Archive<?> targetArchive = deploymentProvider.getClientDeployment(TARGET_MODULE_NAME);
        String targetDeploymentName = archiveDeployer.deploy(targetArchive);
        assertNotNull("Deployment name not null", targetDeploymentName);
        try {
            // Check that the target service is up
            ServiceName targetService = ServiceName.parse("jboss.osgi.example.target.service");
            assertServiceState(targetService, State.UP, 5000);

            // Register the target module with the OSGi layer
            Bundle targetBundle = registerModule(ModuleIdentifier.create("deployment." + targetDeploymentName));
            try
            {
                assertEquals("Bundle INSTALLED", Bundle.INSTALLED, targetBundle.getState());

                // Install the client bundle
                InputStream input = deploymentProvider.getClientDeploymentAsStream(CLIENT_BUNDLE_NAME);
                Bundle clientBundle = context.installBundle(CLIENT_BUNDLE_NAME, input);
                assertEquals("Bundle INSTALLED", Bundle.INSTALLED, clientBundle.getState());
                try {
                    // Start the client bundle, which calls the target service.
                    clientBundle.start();
                    assertEquals("Bundle ACTIVE", Bundle.ACTIVE, clientBundle.getState());
                } finally {
                    // Uninstall the client bundle
                    clientBundle.uninstall();
                }
            }
            finally {
                // Uninstall the target bundle
                targetBundle.uninstall();
            }
        } finally {
            // Undeploy the target module
            archiveDeployer.undeploy(targetDeploymentName);
        }
    }

    @ArchiveProvider
    public static JavaArchive getTestArchive(String name) throws Exception {
        if (CLIENT_BUNDLE_NAME.equals(name))
            return getClientBundleArchive();
        if (TARGET_MODULE_NAME.equals(name))
            return getTargetModuleArchive();
        return null;
    }

    private static JavaArchive getClientBundleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CLIENT_BUNDLE_NAME);
        archive.addClasses(ClientBundleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addBundleActivator(ClientBundleActivator.class);
                builder.addImportPackages(BundleActivator.class, Logger.class, Echo.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    private static JavaArchive getTargetModuleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, TARGET_MODULE_NAME);
        archive.addClasses(Echo.class, EchoService.class, TargetModuleActivator.class);
        String activatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addResource(OSGiTestHelper.getResourceFile("osgi/xservice/target-module/" + activatorPath), activatorPath);
        archive.setManifest(OSGiTestHelper.getResourceFile("osgi/xservice/target-module/" + JarFile.MANIFEST_NAME));
        return archive;
    }
}

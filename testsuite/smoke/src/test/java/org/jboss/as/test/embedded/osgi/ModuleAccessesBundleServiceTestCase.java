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
import org.jboss.as.test.embedded.osgi.bundle.TargetBundleActivator;
import org.jboss.as.test.embedded.osgi.bundle.TargetBundleEchoImpl;
import org.jboss.as.test.embedded.osgi.module.ClientModuleActivator;
import org.jboss.as.test.embedded.osgi.module.EchoInvokerService;
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
 * A test that shows how a module can access an OSGi service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
@RunWith(Arquillian.class)
public class ModuleAccessesBundleServiceTestCase extends AbstractXServiceTestCase {

    private static final String CLIENT_MODULE_NAME = "example-xservice-client-module";
    private static final String TARGET_BUNDLE_NAME = "example-xservice-target-bundle";
    private static final String API_MODULE_NAME = "example-xservice-api-module";

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrapUtils.createJavaArchive("osgi/xservice/bundle-access-test.jar", AbstractXServiceTestCase.class);
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
    public void moduleInvokesBundleService() throws Exception {
        // Deploy the module that contains the API
        Archive<?> apiArchive = deploymentProvider.getClientDeployment(API_MODULE_NAME);
        String apiDeploymentName = archiveDeployer.deploy(apiArchive);
        assertNotNull("Deployment name not null", apiDeploymentName);
        try {
            // Register the API module with the OSGi layer
            Bundle apiBundle = registerModule(ModuleIdentifier.create("deployment." + apiDeploymentName));
            try
            {
                assertEquals("Bundle INSTALLED", Bundle.INSTALLED, apiBundle.getState());

                // Install the bundle that contains the target service
                InputStream input = deploymentProvider.getClientDeploymentAsStream(TARGET_BUNDLE_NAME);
                Bundle targetBundle = context.installBundle(TARGET_BUNDLE_NAME, input);
                assertEquals("Bundle INSTALLED", Bundle.INSTALLED, targetBundle.getState());
                try {
                    // Start the target service bundle
                    targetBundle.start();
                    assertEquals("Bundle ACTIVE", Bundle.ACTIVE, targetBundle.getState());

                    // Deploy the non-osgi client module
                    Archive<?> clientArchive = deploymentProvider.getClientDeployment(CLIENT_MODULE_NAME);
                    String clientDeploymentName = archiveDeployer.deploy(clientArchive);
                    assertNotNull("Deployment name not null", clientDeploymentName);
                    try {
                        // Wait for the client service to come up
                        assertServiceState(ServiceName.parse("jboss.osgi.example.invoker.service"), State.UP, 5000);
                    } finally {
                        // Undeploy the client module
                        archiveDeployer.undeploy(clientDeploymentName);
                    }
                } finally {
                    // Uninstall the target bundle
                    targetBundle.uninstall();
                }
            }
            finally
            {
                // Uninstall the API bundle
                apiBundle.uninstall();
            }
        } finally {
            // Undeploy the API module
            archiveDeployer.undeploy(apiDeploymentName);
        }
    }

    @ArchiveProvider
    public static JavaArchive getTestArchive(String name) throws Exception {
        if (API_MODULE_NAME.equals(name))
            return getAPIModuleArchive();
        if (TARGET_BUNDLE_NAME.equals(name))
            return getTargetBundleArchive();
        if (CLIENT_MODULE_NAME.equals(name))
            return getClientModuleArchive();
        return null;
    }

    private static JavaArchive getAPIModuleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, API_MODULE_NAME);
        archive.addClasses(Echo.class);
        archive.addDirectory("META-INF");
        return archive;
    }

    private static JavaArchive getTargetBundleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, TARGET_BUNDLE_NAME);
        archive.addClasses(TargetBundleActivator.class, TargetBundleEchoImpl.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addBundleActivator(TargetBundleActivator.class);
                builder.addImportPackages(BundleActivator.class, Logger.class, Echo.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    private static JavaArchive getClientModuleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CLIENT_MODULE_NAME);
        archive.addClasses(EchoInvokerService.class, ClientModuleActivator.class);
        String activatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addResource(OSGiTestHelper.getResourceFile("osgi/xservice/client-module/" + activatorPath), activatorPath);
        archive.setManifest(OSGiTestHelper.getResourceFile("osgi/xservice/client-module/" + JarFile.MANIFEST_NAME));
        return archive;
    }
}

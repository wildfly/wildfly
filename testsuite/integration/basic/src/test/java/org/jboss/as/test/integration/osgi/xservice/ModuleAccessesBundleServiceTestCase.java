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

import static org.jboss.as.test.osgi.OSGiFrameworkUtils.getDeployedBundle;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.xservice.api.Echo;
import org.jboss.as.test.integration.osgi.xservice.bundle.TargetBundleActivator;
import org.jboss.as.test.integration.osgi.xservice.module.ClientModuleTwoActivator;
import org.jboss.as.test.osgi.OSGiFrameworkUtils;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

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
        archive.addClasses(OSGiFrameworkUtils.class, AbstractXServiceTestCase.class);
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
    public BundleContext context;

    @Test
    public void moduleInvokesBundleService() throws Exception {

        // Deploy the bundle which contains the target service
        deployer.deploy(TARGET_BUNDLE_NAME);
        try {
            // Find the installed bundle using PackageAdmin
            Bundle targetBundle = getDeployedBundle(context, TARGET_BUNDLE_NAME, null);
            targetBundle.start();

            // Install the client module
            deployer.deploy(CLIENT_MODULE_NAME);
            try {
                // Check that the client service is up
                ServiceName clientService = ServiceName.parse("jboss.osgi.example.invoker.service");
                assertServiceState(serviceContainer, clientService, State.UP, 5000);
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
        archive.addAsServiceProvider(ServiceActivator.class, ClientModuleTwoActivator.class);
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

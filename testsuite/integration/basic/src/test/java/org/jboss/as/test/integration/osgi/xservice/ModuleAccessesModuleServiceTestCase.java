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
import org.jboss.as.test.integration.osgi.xservice.module.ClientModuleOneActivator;
import org.jboss.as.test.integration.osgi.xservice.module.EchoService;
import org.jboss.as.test.integration.osgi.xservice.module.TargetModuleActivator;
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

import javax.inject.Inject;
import java.io.InputStream;

/**
 * A test that shows how a module can access another module's service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2011
 */
@RunWith(Arquillian.class)
public class ModuleAccessesModuleServiceTestCase extends AbstractXServiceTestCase {

    private static final String TARGET_MODULE_NAME = "example-xservice-mam-target-module";
    private static final String CLIENT_MODULE_NAME = "example-xservice-mam-client-module";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "xservice-module-access");
        archive.addClasses(AbstractXServiceTestCase.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Logger.class, Module.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Inject
    public ServiceContainer serviceContainer;

    @ArquillianResource
    public Deployer deployer;

    @Override
    ServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    @Test
    public void moduleInvokesModuleService() throws Exception {
        // Deploy the module which contains the target service
        deployer.deploy(TARGET_MODULE_NAME);
        try {
            // Check that the target service is up
            ServiceName targetService = ServiceName.parse("jboss.osgi.example.target.service");
            assertServiceState(targetService, State.UP, 5000);

            // Install the client module
            deployer.deploy(CLIENT_MODULE_NAME);
            try
            {
                // Check that the client service is up
                ServiceName clientService = ServiceName.parse("jboss.osgi.example.invoker.service");
                assertServiceState(clientService, State.UP, 5000);
            }
            finally {
                // Undeploy the client module
                deployer.undeploy(CLIENT_MODULE_NAME);
            }
        } finally {
            // Undeploy the target module
            deployer.undeploy(TARGET_MODULE_NAME);
        }
    }

    @Deployment(name = CLIENT_MODULE_NAME, managed = false, testable = false)
    public static JavaArchive getClientModuleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CLIENT_MODULE_NAME);
        archive.addClasses(ClientModuleOneActivator.class);
        String activatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addAsResource("osgi/xservice/client-module-one/" + activatorPath, activatorPath);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.jboss.modules,org.jboss.logging,deployment.example-xservice-mam-target-module");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = TARGET_MODULE_NAME, managed = false, testable = false)
    public static JavaArchive getTargetModuleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, TARGET_MODULE_NAME);
        archive.addClasses(Echo.class, EchoService.class, TargetModuleActivator.class);
        String activatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addAsResource("osgi/xservice/target-module/" + activatorPath, activatorPath);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.jboss.modules,org.jboss.logging");
                return builder.openStream();
            }
        });
        return archive;
    }
}

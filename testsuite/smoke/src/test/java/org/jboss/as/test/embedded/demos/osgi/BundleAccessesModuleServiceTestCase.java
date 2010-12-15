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

package org.jboss.as.test.embedded.demos.osgi;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.jar.JarFile;

import org.jboss.as.test.embedded.demos.osgi.api.Echo;
import org.jboss.as.test.embedded.demos.osgi.bundle.ClientBundleActivator;
import org.jboss.as.test.embedded.demos.osgi.module.EchoService;
import org.jboss.as.test.embedded.demos.osgi.module.TargetModuleActivator;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.osgi.testing.OSGiBundle;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * A test that shows how a bundle can access an MSC service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
@Ignore("Migrate to Arquillian when there is support for multiple test deployments")
public class BundleAccessesModuleServiceTestCase extends AbstractXServiceTestCase {
    @Test
    public void bundleInvokesModuleService() throws Exception {
        // Deploy the non-OSGi module which contains the target service
        JavaArchive targetArchive = getTargetModuleArchive();
        String targetDeploymentName = getRemoteRuntime().deploy(targetArchive);
        assertNotNull("Deployment name not null", targetDeploymentName);
        try {
            // Register the target module with the OSGi layer
            registerModule(ModuleIdentifier.create("deployment." + targetDeploymentName));

            // Install the client bundle
            OSGiBundle clientBundle = getRemoteRuntime().installBundle(getClientBundleArchive());
            assertBundleState(Bundle.INSTALLED, clientBundle.getState());
            try {
                // Start the client bundle, which calls the target service. Check the console log for echo message
                clientBundle.start();
                assertBundleState(Bundle.ACTIVE, clientBundle.getState());
            } finally {
                // Uninstall the client bundle
                clientBundle.uninstall();
            }
        } finally {
            // Undeploy the target module
            getRemoteRuntime().undeploy(targetDeploymentName);
        }
    }

    private JavaArchive getClientBundleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-xservice-client-bundle");
        archive.addClasses(ClientBundleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addBundleActivator(ClientBundleActivator.class);
                builder.addImportPackages(Echo.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    private JavaArchive getTargetModuleArchive() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-xservice-target-module");
        archive.addClasses(Echo.class, EchoService.class, TargetModuleActivator.class);
        String activatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addResource(getResourceFile("osgi/xservice/target-module/" + activatorPath), activatorPath);
        archive.setManifest(getResourceFile("osgi/xservice/target-module/" + JarFile.MANIFEST_NAME));
        return archive;
    }
}

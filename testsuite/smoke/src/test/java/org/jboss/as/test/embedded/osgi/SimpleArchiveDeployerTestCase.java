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

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.api.ArchiveDeployer;
import org.jboss.arquillian.api.ArchiveProvider;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.DeploymentProvider;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.embedded.osgi.bundle.SimpleActivator;
import org.jboss.as.test.embedded.osgi.bundle.SimpleService;
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
 * Test the arquillian callback to a client provided archive
 * and its deployment through the deployer API.
 *
 * @author thomas.diesler@jboss.com
 * @since 09-Sep-2010
 */
@RunWith(Arquillian.class)
public class SimpleArchiveDeployerTestCase {

    @Inject
    public DeploymentProvider provider;

    @Inject
    public ArchiveDeployer archiveDeployer;

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-deployment-provider");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                // [TODO] remove these explicit imports
                builder.addImportPackages("org.jboss.shrinkwrap.impl.base.path");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testClientDeploymentAsArchive() throws Exception {

        String symbolicName = "archive-deployer-test-bundle";
        Archive<?> archive = provider.getClientDeployment(symbolicName);
        String depname = archiveDeployer.deploy(archive);
        Bundle bundle = null;
        try {
            for (Bundle aux : context.getBundles()) {
                if (symbolicName.equals(aux.getSymbolicName())) {
                    bundle = aux;
                    break;
                }
            }
            // Assert that the bundle is there
            assertNotNull("Bundle found", bundle);

            // Start the bundle
            bundle.start();
            OSGiTestHelper.assertBundleState(Bundle.ACTIVE, bundle.getState());

            // Stop the bundle
            bundle.stop();
            OSGiTestHelper.assertBundleState(Bundle.RESOLVED, bundle.getState());
        } finally {
            archiveDeployer.undeploy(depname);
            // FIXME JBAS-9359
            // OSGiTestHelper.assertBundleState(Bundle.UNINSTALLED, bundle.getState());
        }
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
                builder.addBundleActivator(SimpleActivator.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

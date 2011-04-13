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
package org.jboss.as.testsuite.integration.osgi;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.api.ArchiveDeployer;
import org.jboss.arquillian.api.ArchiveProvider;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.DeploymentProvider;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.osgi.bundle.SimpleActivator;
import org.jboss.as.testsuite.integration.osgi.bundle.SimpleService;
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
 * Bundle gets installed through {@link BundleContext#installBundle(String, InputStream)} and gets uninstalled through
 * {@link Bundle#uninstall()}
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2011
 */
@RunWith(Arquillian.class)
public class BundleDeploymentCaseTwoTestCase {

    @Inject
    public DeploymentProvider provider;

    @Inject
    public BundleContext context;

    @Inject
    public ArchiveDeployer archiveDeployer;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundle-deployment-casetwo");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                // [TODO] Remove these explicit imports
                builder.addImportPackages("org.jboss.shrinkwrap.api.exporter", "org.jboss.shrinkwrap.impl.base.path");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleDeployment() throws Exception {

        Archive<?> bundleArchive = provider.getClientDeployment("test-bundle-two");
        String deploymentName = archiveDeployer.deploy(bundleArchive);
        assertNotNull("Deployment name not null", deploymentName);
        Bundle bundle = null;
        try {

            // Find the deployed bundle
            for (Bundle aux : context.getBundles()) {
                if (aux.getSymbolicName().equals(bundleArchive.getName()))
                    bundle = aux;
            }

            // Start the bundle. Note, it may have started already
            bundle.start();
            OSGiTestHelper.assertBundleState(Bundle.ACTIVE, bundle.getState());

            // Stop the bundle
            bundle.stop();
            OSGiTestHelper.assertBundleState(Bundle.RESOLVED, bundle.getState());
        } finally {
            archiveDeployer.undeploy(deploymentName);
            OSGiTestHelper.assertBundleState(Bundle.UNINSTALLED, bundle.getState());
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

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
package org.jboss.as.test.integration.osgi.deployment;

import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.xservice.bundle.SimpleActivator;
import org.jboss.as.test.integration.osgi.xservice.bundle.SimpleService;
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

/**
 * Bundle gets installed through {@link BundleContext#installBundle(String, InputStream)} and
 * gets uninstalled through {@link Bundle#uninstall()}
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2011
 */
@RunWith(Arquillian.class)
public class BundleDeploymentCaseOneTestCase {

    static final String BUNDLE_DEPLOYMENT_NAME = "test-bundle-one";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundle-deployment-case-one");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleDeployment() throws Exception {

        InputStream input = deployer.getDeployment(BUNDLE_DEPLOYMENT_NAME);
        Bundle bundle = context.installBundle("test-bundle", input);
        try {
            // Assert that the bundle is in state INSTALLED
            Assert.assertEquals(Bundle.INSTALLED, bundle.getState());

            // Start the bundle
            bundle.start();
            Assert.assertEquals(Bundle.ACTIVE, bundle.getState());

            // Stop the bundle
            bundle.stop();
            Assert.assertEquals(Bundle.RESOLVED, bundle.getState());
        } finally {
            bundle.uninstall();
            Assert.assertEquals(Bundle.UNINSTALLED, bundle.getState());
        }
    }

    @Deployment(name = BUNDLE_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_DEPLOYMENT_NAME);
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

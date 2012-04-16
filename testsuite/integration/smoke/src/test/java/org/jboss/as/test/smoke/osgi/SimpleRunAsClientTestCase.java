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
package org.jboss.as.test.smoke.osgi;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.osgi.OSGiManagementTest;
import org.jboss.as.test.smoke.osgi.bundle.SimpleActivator;
import org.jboss.as.test.smoke.osgi.bundle.SimpleService;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleActivator;

/**
 * Test deployer API and OSGi management operations.
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SimpleRunAsClientTestCase extends OSGiManagementTest {

    private static final String DEPLOYMENT_NAME = "runasclient-test-bundle";

    @ArquillianResource
    public Deployer deployer;

    @Test
    public void testClientDeploymentAsArchive() throws Exception {

        deployer.deploy(DEPLOYMENT_NAME);
        Long bundleId = getBundleId(DEPLOYMENT_NAME, null);
        Assert.assertNotNull("Bundle found", bundleId);
        Assert.assertEquals("INSTALLED", getBundleState(bundleId));

        Assert.assertTrue("Bundle started", bundleStart(bundleId));
        Assert.assertEquals("ACTIVE", getBundleState(bundleId));

        Assert.assertTrue("Bundle stopped", bundleStop(bundleId));
        Assert.assertEquals("RESOLVED", getBundleState(bundleId));

        deployer.undeploy(DEPLOYMENT_NAME);
        Assert.assertNull("UNINSTALLED", getBundleState(bundleId));
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME);
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

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

import static org.jboss.as.test.osgi.OSGiManagementOperations.bundleStart;
import static org.jboss.as.test.osgi.OSGiManagementOperations.bundleStop;
import static org.jboss.as.test.osgi.OSGiManagementOperations.getBundleId;
import static org.jboss.as.test.osgi.OSGiManagementOperations.getBundleInfo;
import static org.jboss.as.test.osgi.OSGiManagementOperations.getBundleState;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.osgi.parser.ModelConstants;
import org.jboss.as.test.smoke.osgi.bundle.SimpleActivator;
import org.jboss.as.test.smoke.osgi.bundle.SimpleService;
import org.jboss.dmr.ModelNode;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
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
public class SimpleRunAsClientTestCase {

    private static final String DEPLOYMENT_NAME = "runasclient-test-bundle";
    private static final String SYMBOLIC_NAME = "test-bundle";

    @ArquillianResource
    public Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        archive.addClass(SimpleRunAsClientTestCase.class);
        return archive;
    }

    @Test
    public void testClientDeploymentAsArchive() throws Exception {

        deployer.deploy(DEPLOYMENT_NAME);
        try {
            Long bundleId = getBundleId(getControllerClient(), SYMBOLIC_NAME, null);
            Assert.assertNotNull("Bundle found", bundleId);
            Assert.assertEquals("INSTALLED", getBundleState(getControllerClient(), bundleId));

            Assert.assertTrue("Bundle started", bundleStart(getControllerClient(), bundleId));
            Assert.assertEquals("ACTIVE", getBundleState(getControllerClient(), bundleId));

            ModelNode info = getBundleInfo(getControllerClient(), bundleId);
            Assert.assertEquals(7, info.asList().size());
            Assert.assertEquals(bundleId, (Long)info.get(ModelConstants.ID).asLong());
            Assert.assertEquals(1, info.get(ModelConstants.STARTLEVEL).asInt());
            Assert.assertEquals("ACTIVE", info.get(ModelConstants.STATE).asString());
            Assert.assertEquals(SYMBOLIC_NAME, info.get(ModelConstants.SYMBOLIC_NAME).asString());
            Assert.assertEquals(DEPLOYMENT_NAME, info.get(ModelConstants.LOCATION).asString());
            Assert.assertEquals("bundle", info.get(ModelConstants.TYPE).asString());
            Assert.assertEquals("0.0.0", info.get(ModelConstants.VERSION).asString());

            Assert.assertTrue("Bundle stopped", bundleStop(getControllerClient(), bundleId));
            Assert.assertEquals("RESOLVED", getBundleState(getControllerClient(), bundleId));

            Assert.assertTrue("Bundle started", bundleStart(getControllerClient(), DEPLOYMENT_NAME));
            Assert.assertEquals("ACTIVE", getBundleState(getControllerClient(), DEPLOYMENT_NAME));

            Assert.assertTrue("Bundle stopped", bundleStop(getControllerClient(), DEPLOYMENT_NAME));
            Assert.assertEquals("RESOLVED", getBundleState(getControllerClient(), DEPLOYMENT_NAME));
        } finally {
            deployer.undeploy(DEPLOYMENT_NAME);
        }
    }

    private ModelControllerClient getControllerClient() {
        return managementClient.getControllerClient();
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME);
        archive.addClasses(SimpleActivator.class, SimpleService.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(SYMBOLIC_NAME);
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(SimpleActivator.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

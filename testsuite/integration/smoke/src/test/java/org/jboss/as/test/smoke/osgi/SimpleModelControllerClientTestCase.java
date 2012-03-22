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

import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_METADATA_STARTLEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.test.osgi.OSGiTestSupport;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * Test bundle deployment using the {@link ModelControllerClient}
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Mar-2012
 */
@RunWith(Arquillian.class)
public class SimpleModelControllerClientTestCase {

    static final String BUNDLE_DEPLOYMENT_NAME = "test-bundle";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public StartLevel startLevel;

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundle");
        archive.addClasses(OSGiTestSupport.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ClientConstants.class, ModelControllerClient.class, DeploymentPlanBuilder.class);
                builder.addImportPackages(PackageAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testManagementClientInjected() throws Exception {

        ModelControllerClient client = getModelControllerClient();
        assertNotNull("ModelControllerClient available", client);

        InputStream input = deployer.getDeployment(BUNDLE_DEPLOYMENT_NAME);
        ServerDeploymentHelper server = new ServerDeploymentHelper(client);
        Map<String, Object> userdata = new HashMap<String, Object>();
        userdata.put(DEPLOYMENT_METADATA_STARTLEVEL, Integer.valueOf(20));
        String runtimeName = server.deploy(BUNDLE_DEPLOYMENT_NAME, input, userdata);

        // Find the deployed bundle
        Bundle bundle = OSGiTestSupport.getDeployedBundle(context, BUNDLE_DEPLOYMENT_NAME, null);
        assertNotNull("Bundle installed", bundle);

        int bundleStartLevel = startLevel.getBundleStartLevel(bundle);
        assertEquals("Bundle @ given level", 20, bundleStartLevel);

        server.undeploy(runtimeName);
    }

    private ModelControllerClient getModelControllerClient() {
        ServiceReference sref = context.getServiceReference(ModelControllerClient.class.getName());
        return (ModelControllerClient) context.getService(sref);
    }

    @Deployment(name = BUNDLE_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_DEPLOYMENT_NAME);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                return builder.openStream();
            }
        });
        return archive;
    }
}

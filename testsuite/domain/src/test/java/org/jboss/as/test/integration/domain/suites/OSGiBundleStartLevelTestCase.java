/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.domain.DomainDeploymentHelper;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test bundle install in a higher start level.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 23-Mar-2012
 */
public class OSGiBundleStartLevelTestCase extends AbstractOSGiTestCase {

    private static DomainTestSupport testSupport;
    private static String webAppRuntimeName;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(OSGiBundleStartLevelTestCase.class.getSimpleName());
        webAppRuntimeName = deployHttpEndpoint(testSupport);
    }

    @AfterClass
    public static void teardownDomain() throws Exception {
        undeployHttpEndpoint(testSupport, webAppRuntimeName);
        DomainTestSuite.stopSupport();
        testSupport = null;
    }

    @Test
    public void testBundleStartLevel() throws Exception {

        // Test http endpoint without attached osgi service
        String response = HttpRequest.get(getHttpEndpointURL() + "?bnd=test-bundle", 10, 10, TimeUnit.SECONDS);
        Assert.assertEquals("Bundle not available: test-bundle", response);

        // Setup the deployment metadata
        Map<String, Object> userdata = new HashMap<String, Object>();
        userdata.put(DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL, Integer.valueOf(20));

        // Deploy the service bundle
        JavaArchive archive = getBundleArchive();
        InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
        DomainDeploymentHelper domainDeployer = getDomainDeployer(testSupport);
        String runtimeName = domainDeployer.deploy(archive.getName(), input, userdata, SERVER_GROUPS);
        try {
            response = HttpRequest.get(getHttpEndpointURL() + "?bnd=test-bundle&cmd=startlevel", 10, TimeUnit.SECONDS);
            Assert.assertEquals("test-bundle:0.0.0: startlevel==20", response);
        } finally {
            domainDeployer.undeploy(runtimeName, SERVER_GROUPS);
        }
    }

    private JavaArchive getBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test-bundle");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }
}

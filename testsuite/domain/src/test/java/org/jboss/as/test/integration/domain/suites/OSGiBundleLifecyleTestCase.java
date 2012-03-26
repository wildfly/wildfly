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

import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.domain.DomainDeploymentHelper;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.osgi.bundle.FeedbackActivator;
import org.jboss.as.test.integration.domain.osgi.webapp.FeedbackService;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleActivator;

/**
 * Test of various management operations for OSGi bundles in a domain.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 23-Mar-2012
 */
public class OSGiBundleLifecyleTestCase extends AbstractOSGiTestCase {

    @Test
    public void testBundleActive() throws Exception {

        // Test http endpoint without attached osgi service
        String spec = "http://" + DomainTestSupport.slaveAddress + ":" + 8630 + "/test-webapp/feedback";
        String response = HttpRequest.get(spec, 10, 10, TimeUnit.SECONDS);
        Assert.assertEquals("FeedbackService not available", response);

        // Deploy the service bundle
        JavaArchive archive = getGoodBundleArchive();
        InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
        DomainDeploymentHelper domain = new DomainDeploymentHelper(deploymentManager);
        String runtimeName = domain.deploy(archive.getName(), input, SERVER_GROUPS);
        try {
            // Verify bundle startlevel through http endpoint
            response = HttpRequest.get(spec + "?bnd=good-bundle&cmd=hello", 10, TimeUnit.SECONDS);
            Assert.assertEquals("good-bundle:0.0.0: hello", response);
        } finally {
            domain.undeploy(runtimeName, SERVER_GROUPS);
        }
    }

    @Test
    @Ignore("[AS7-4292] Invalid bundle deployment does not fail in domain")
    public void testInvalidBundleDeployment() throws Exception {
        JavaArchive archive = getBadBundleArchive();
        InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
        DomainDeploymentHelper domain = new DomainDeploymentHelper(deploymentManager);
        try {
            domain.deploy(archive.getName(), input, SERVER_GROUPS);
            fail("Deployment exception expected");
        } catch (Exception ex) {
            // expected
        }
    }

    private JavaArchive getGoodBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "good-bundle");
        archive.addClasses(FeedbackActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(FeedbackActivator.class);
                builder.addImportPackages(BundleActivator.class, FeedbackService.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    private JavaArchive getBadBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bad-bundle");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addManifestHeader(BUNDLE_VERSION, "bogus");
                return builder.openStream();
            }
        });
        return archive;
    }
}

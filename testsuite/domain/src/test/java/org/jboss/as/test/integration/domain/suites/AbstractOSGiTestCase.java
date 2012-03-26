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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.domain.DomainDeploymentHelper;
import org.jboss.as.controller.client.helpers.domain.DomainDeploymentManager;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.osgi.webapp.FeedbackService;
import org.jboss.as.test.integration.domain.osgi.webapp.FeedbackServlet;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;


/**
 * Abstract test base for OSGi test cases
 *
 * @author Thomas.Diesler@jboss.com
 * @since 23-Mar-2012
 */
public abstract class AbstractOSGiTestCase {

    static List<String> SERVER_GROUPS = Collections.singletonList("other-server-group");

    static DomainClient domainClient;
    static DomainDeploymentManager deploymentManager;
    private static String webAppRuntimeName;

    @Before
    public void setupDomain() throws Exception {
        if (webAppRuntimeName == null) {
            DomainTestSupport testSupport = DomainTestSuite.createSupport(getClass().getSimpleName());
            domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
            deploymentManager = domainClient.getDeploymentManager();

            // Deploy the http endpoint
            WebArchive webArchive = getWebArchive();
            InputStream webInput = webArchive.as(ZipExporter.class).exportAsInputStream();
            DomainDeploymentHelper domain = new DomainDeploymentHelper(deploymentManager);
            webAppRuntimeName = domain.deploy(webArchive.getName(), webInput, null, SERVER_GROUPS);
        }
    }

    @AfterClass
    public static void teardownDomain() throws Exception {
        DomainDeploymentHelper domain = new DomainDeploymentHelper(deploymentManager);
        domain.undeploy(webAppRuntimeName, SERVER_GROUPS);
    }

    static WebArchive getWebArchive() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "test-webapp.war");
        archive.addClasses(FeedbackServlet.class, FeedbackService.class);
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,org.jboss.osgi.framework");
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }
}

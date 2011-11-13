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

package org.jboss.as.test.integration.osgi.webapp;

import static org.junit.Assert.assertEquals;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.HttpTestSupport;
import org.jboss.as.test.integration.osgi.xservice.api.Echo;
import org.jboss.as.test.integration.osgi.xservice.bundle.TargetBundleActivator;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Testcase for basic Web / OSGi integration
 *
 * @author thomas.diesler@jboss.com
 * @since 13-May-2011
 */
@RunWith(Arquillian.class)
public class ServletIntegrationTestCase {

    static final String WAR_DEPLOYMENT_NAME = "web-osgi-client.war";

    @Inject
    public Bundle bundle;

    @ArquillianResource
    public Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "web-osgi-target");
        archive.addClasses(HttpTestSupport.class, Echo.class, TargetBundleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(TargetBundleActivator.class);
                builder.addImportPackages(BundleActivator.class, Logger.class, Module.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testTargetBundle() throws Exception {
        bundle.start();
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(Echo.class.getName());
        Echo service = (Echo) context.getService(sref);
        assertEquals("foo", service.echo("foo"));
    }

    @Test
    public void testServiceAccess() throws Exception {
        deployer.deploy(WAR_DEPLOYMENT_NAME);
        try {
            assertEquals("web-osgi-target", getHttpResponse(BUNDLE_SYMBOLICNAME));
            assertEquals("foo", getHttpResponse("foo"));
        } finally {
            deployer.undeploy(WAR_DEPLOYMENT_NAME);
        }
    }

    @Deployment(name = WAR_DEPLOYMENT_NAME, managed = false, testable = false)
    public static WebArchive getTestArchive() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_NAME);
        archive.addClass(SimpleClientServlet.class);
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,deployment.web-osgi-target:0.0.0");
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }

    private String getHttpResponse(String message) throws IOException {
        String reqPath = "/web-osgi-client/servlet?msg=" + message;
        return HttpTestSupport.getHttpResponse("localhost", 8080, reqPath, 2000);
    }
}

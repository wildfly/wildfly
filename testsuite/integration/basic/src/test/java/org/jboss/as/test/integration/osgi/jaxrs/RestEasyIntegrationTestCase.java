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

package org.jboss.as.test.integration.osgi.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.xservice.api.Echo;
import org.jboss.as.test.integration.osgi.xservice.bundle.TargetBundleActivator;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
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

import static org.junit.Assert.assertEquals;

/**
 * [AS7-1974] Unable to access OSGI service from RESTEasy WebApp
 *
 * @author thomas.diesler@jboss.com
 * @since 08-Oct-2011
 */
@RunWith(Arquillian.class)
public class RestEasyIntegrationTestCase {

    static final String DEPLOYMENT_NAME = "resteasy-osgi-client.war";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jaxrs-osgi-target");
        archive.addClasses(HttpRequest.class, Echo.class, TargetBundleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(TargetBundleActivator.class);
                builder.addImportPackages(BundleActivator.class, ModuleIdentifier.class, Logger.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false, testable = false)
    public static WebArchive endpointWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        archive.addClass(SimpleRestEndpoint.class);
        archive.setWebXML("osgi/jaxrs/web.xml");
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,deployment.jaxrs-osgi-target:0.0.0");
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }

    @Test
    public void testServiceAccess() throws Exception {

        // [AS7-2178] Deployment cannot create dependency on unresolved OSGi bundle
        bundle.start();

        deployer.deploy(DEPLOYMENT_NAME);
        try {
            assertEquals("kermit", getHttpResponse("kermit"));
        } finally {
            deployer.undeploy(DEPLOYMENT_NAME);
        }
    }

    private String getHttpResponse(String message) throws IOException, ExecutionException, TimeoutException {
        String reqPath = "http://" + System.getProperty("test.bind.address", "localhost") + ":8080/resteasy-osgi-client/rest/echo/" + message;
        return HttpRequest.get(reqPath, 10, TimeUnit.SECONDS);
    }

}

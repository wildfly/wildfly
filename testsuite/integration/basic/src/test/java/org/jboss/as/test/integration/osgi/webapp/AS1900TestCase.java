/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import javax.servlet.Filter;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.HttpTestSupport;
import org.jboss.as.test.integration.osgi.webapp.bundle.EndpointFilter;
import org.jboss.as.test.integration.osgi.webapp.bundle.EndpointServlet;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-1900] Inconsistent class space with package from javaee.api
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Sep-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class AS1900TestCase {

    @ArquillianResource
    public Deployer deployer;

    @Test
    public void testServletAccess() throws Exception {
        deployer.deploy("as1900-bundle");
        deployer.deploy("as1900-webapp");
        String line = getHttpResponse("/as1900/servlet?test=plain", 5000);
        assertEquals("(filtered)Hello from Servlet", line);
        deployer.undeploy("as1900-webapp");
        deployer.undeploy("as1900-bundle");
    }

    @Deployment(name = "as1900-webapp", managed = false, testable = false)
    public static WebArchive getWarArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "as1900.war");
        archive.addClasses(EndpointServlet.class);
        archive.addAsWebInfResource("osgi/webapp/webB.xml", "web.xml");
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "deployment.as1900-bundle:0.0.0");
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }

    @Deployment(name = "as1900-bundle", managed = false, testable = false)
    public static JavaArchive getBundleArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "as1900-bundle");
        archive.addClasses(EndpointFilter.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Filter.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    private String getHttpResponse(String reqPath, int timeout) throws IOException {
        return HttpTestSupport.getHttpResponse("localhost", 8080, reqPath, timeout);
    }
}

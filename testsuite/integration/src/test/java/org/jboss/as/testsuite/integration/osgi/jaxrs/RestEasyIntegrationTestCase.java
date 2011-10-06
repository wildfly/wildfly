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

package org.jboss.as.testsuite.integration.osgi.jaxrs;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.osgi.OSGiTestSupport;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;

/**
 * [AS7-1974] Unable to access OSGI service from RESTEasy WebApp
 *
 * @author thomas.diesler@jboss.com
 * @since 08-Oct-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class RestEasyIntegrationTestCase extends OSGiTestSupport {

    static final String WAR_DEPLOYMENT_NAME = "resteasy-osgi-client.war";

    @Deployment
    // [TODO] Dependency on compendium only works when the OSGi subsystem is up
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_NAME);
        archive.addClass(SimpleRestEndpoint.class);
        archive.setWebXML("osgi/jaxrs/web.xml");
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,org.jboss.osgi.framework,deployment.osgi.cmpn:4.2.0.200908310645");
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }

    @Test
    public void testServiceAccess() throws Exception {
        assertEquals("[\"org.apache.felix.webconsole.internal.servlet.OsgiManager\"]", getHttpResponse());
    }

    private String getHttpResponse() throws IOException {
        String reqPath = "/resteasy-osgi-client/cm/pids";
        return getHttpResponse("localhost", 8080, reqPath, 2000);
    }
}

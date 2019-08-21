/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.secman.jerseyclient;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.testsuite.integration.secman.subsystem.ReloadableCliTestBase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URL;
import java.security.SecurityPermission;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests if ClassNotFoundException is thrown over org.glassfish.jersey.client.JerseyClientBuilder when security manager is enabled.
 * Test for [ WFLY-12235 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JerseyClientTestCase extends ReloadableCliTestBase {

    private static final String DEPLOYMENT = "SERVLET_DEPLOYMENT";

    @Deployment(name = DEPLOYMENT, testable = false)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");

        war.addAsManifestResource(createPermissionsXmlAsset(
                // Required for com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider. During <init> there is a
                // reflection test to check for JAXRS 2.0.
                new SecurityPermission("insertProvider"),
                new RuntimePermission("accessDeclaredMembers"),
                new PropertyPermission("management.address", "read"),
                new PropertyPermission("node0", "read"),
                new PropertyPermission("jboss.http.port", "read"),
                // Required for the ClientBuilder.newBuilder() so the ServiceLoader will work
                new FilePermission("<<ALL FILES>>", "read"),
                // Required for the client to connect
                new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")
        ), "permissions.xml");

        war.addClasses(JerseyClientTestServlet.class, TestSuiteEnvironment.class);
        return war;
    }

    @Test
    public void testJerseyClientBuilder(@ArquillianResource URL webAppURL) throws Exception {
        final URI uri = new URI(webAppURL.toExternalForm() + JerseyClientTestServlet.SERVLET_PATH.substring(1));
        Utils.makeCall(uri, HttpServletResponse.SC_OK);
    }
}

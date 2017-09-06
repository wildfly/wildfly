/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.application;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test for web application authentication using Elytron with default server configuration.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BasicAuthnTestCase {

    private static final String NAME = BasicAuthnTestCase.class.getSimpleName();

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleServlet.class)
                .addAsWebInfResource(BasicAuthnTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    /**
     * Tests access without authentication.
     */
    @Test
    public void testUnprotectedAccess(@ArquillianResource URL url) throws Exception {
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCall(url.toURI(), SC_OK));
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCall(new URI(url.toExternalForm() + "foo"), SC_OK));
    }

    /**
     * Tests '*' wildcard in authn-constraints.
     */
    @Test
    public void testAllRolesAllowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "all");

        assertUrlProtected(servletUrl);

        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_OK));
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_OK));
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "guest", "guest", SC_OK));
    }

    /**
     * Test case sensitivity of role in authn-constraints.
     */
    @Test
    public void testCaseSensitiveRole(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "users");

        assertUrlProtected(servletUrl);

        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(servletUrl, "guest", "guest", SC_FORBIDDEN);
    }

    /**
     * Tests single role in authn-constraint.
     */
    @Test
    public void testUser1Allowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        assertUrlProtected(servletUrl);

        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_OK));

        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(servletUrl, "guest", "guest", SC_FORBIDDEN);
    }

    /**
     * Tests empty authn-constraint in web.xml.
     */
    @Test
    public void testNoRoleAllowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "prohibited");

        Utils.makeCall(servletUrl.toURI(), SC_FORBIDDEN);
    }

    private void assertUrlProtected(final URL servletUrl) throws Exception, URISyntaxException, IOException {
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED);
        // wrong password
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password", SC_UNAUTHORIZED);
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "Password1", SC_UNAUTHORIZED);
        // unknown user
        Utils.makeCallWithBasicAuthn(servletUrl, "User1", "password1", SC_UNAUTHORIZED);
    }
}

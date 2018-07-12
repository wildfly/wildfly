/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.web;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.elytron.util.HttpUtil.get;

import java.io.File;
import java.net.SocketPermission;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.wildfly.test.integration.elytron.ejb.AuthenticationTestCase;
import org.wildfly.test.integration.elytron.util.HttpUtil;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Test case to test authentication to web applications, initially programatic authentication.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class WebAuthenticationTestCaseBase {

    @ArquillianResource
    protected URL url;

    @Deployment
    public static Archive<?> deployment() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = AuthenticationTestCase.class.getPackage();
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        return ShrinkWrap.create(WebArchive.class, "websecurity.war")
                .addClass(LoginServlet.class).addClass(HttpUtil.class)
                .addClass(WebAuthenticationTestCaseBase.class)
                .addClasses(ElytronDomainSetup.class, ServletElytronDomainSetup.class)
                .addAsResource(currentPackage, "users.properties", "users.properties")
                .addAsResource(currentPackage, "roles.properties", "roles.properties")
                .addAsWebInfResource(currentPackage, "web.xml", "web.xml")
                .addAsWebInfResource(currentPackage, "jboss-web.xml", "jboss-web.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // AuthenticationTestCase#execute calls ExecutorService#shutdownNow
                        new RuntimePermission("modifyThread"),
                        // AuthenticationTestCase#execute calls sun.net.www.http.HttpClient#openServer under the hood
                        new SocketPermission(SERVER_HOST_PORT, "connect,resolve")
                        ),
                        "permissions.xml");
    }

    protected abstract String getWebXmlName();

    @Test
    public void testProgramaticAuthentication() throws Exception {
        Map<String, String> headers = Collections.emptyMap();
        AtomicReference<String> sessionCookie = new AtomicReference<>();

        // Test Unauthenticated Call is 'null'
        String identity = get(url + "login", headers, 10, SECONDS, sessionCookie);
        assertEquals("Expected Identity", "null", identity);

        // Test Call can login and identity established.
        identity = get(url + "login?action=login&username=user1&password=password1", headers, 10, SECONDS, sessionCookie);
        assertEquals("Expected Identity", "user1", identity);

        // Test follow up call still has identity.
        identity = get(url + "login", headers, 10, SECONDS, sessionCookie);
        assertEquals("Expected Identity", "user1", identity);

        // Logout and check back to 'null'.
        identity = get(url + "login?action=logout", headers, 10, SECONDS, sessionCookie);
        assertEquals("Expected Identity", "null", identity);

        // Once more call to be sure we really are null.
        identity = get(url + "login", headers, 10, SECONDS, sessionCookie);
        assertEquals("Expected Identity", "null", identity);
    }

    static class ElytronDomainSetupOverride extends ElytronDomainSetup {
        public ElytronDomainSetupOverride() {
            super(new File(WebAuthenticationTestCase.class.getResource("users.properties").getFile()).getAbsolutePath(),
                    new File(WebAuthenticationTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath());
        }
    }
}

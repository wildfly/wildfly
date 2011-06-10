/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.spec.ejb3.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.spec.common.HttpRequest;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.util.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.security.Principal;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class EJB3SecurityTestCase {
    private static final Logger log = Logger.getLogger(EJB3SecurityTestCase.class.getName());

    @Deployment
    public static Archive<?> deployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addPackage(WhoAmIBean.class.getPackage())
                .addPackage(HttpRequest.class.getPackage())
                .addClass(Base64.class)
                .addAsResource("security/users.properties", "users.properties")
                .addAsResource("security/roles.properties", "roles.properties")
                .addAsWebInfResource("ejb3/security/web.xml", "web.xml");
        log.info(war.toString(true));
        return war;
    }

    @EJB(mappedName = "java:global/ejb3security/WhoAmIBean")
    private WhoAmIBean whoAmIBean;

    // 17.2.5 - Programatic Access to Caller's Security Context
    // Include tests for methods not implemented to pick up if later they are implemented.
    // 17.2.5.1 - Use of getCallerPrincipal
    // 17.6.5 - Security Methods on EJBContext

    @Test
    public void testAuthenticatedCall() throws Exception {
        // TODO: this is not spec
        final SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("anil", "anil");
        client.login();
        try {
            try {
                final Principal principal = whoAmIBean.getCallerPrincipal();
                assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.", principal);
                assertEquals("anil", principal.getName());
            } catch (RuntimeException e) {
                e.printStackTrace();
                fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method (" + e.getMessage() + ")");
            }
        } finally {
            client.logout();
        }
    }

    @Test
    public void testUnauthenticated() throws Exception {
        try {
            final Principal principal = whoAmIBean.getCallerPrincipal();
            assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.", principal);
            // TODO: where is 'anonymous' configured?
            assertEquals("anonymous", principal.getName());
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method (" + e.getMessage() + ")");
        }
    }

    @Test
    public void testViaServlet() throws Exception {
        final String result = HttpRequest.get("http://localhost:8080/ejb3security/whoAmI", "anil", "anil", 10, SECONDS);
        assertEquals("anil", result);
    }

    // 17.2.5.2 - Use of isCallerInRole
    // 17.2.5.3 - Declaration of Security Roles Referenced from the Bean's Code
    // 17.3.1 - Security Roles
    // 17.3.2.1 - Specification of Method Permissions with Metadata Annotation
    // 17.3.2.2 - Specification of Method Permissions in the Deployment Descriptor
    // 17.3.2.3 - Unspecified Method Permission
    // 17.3.3 - Linking Security Role References to Security Roles
    // 17.3.4 - Specification on Security Identities in the Deployment Descriptor
    //            (Include permutations for overrides esp where deployment descriptor removes access)
    // 17.3.4.1 - Run-as
    // 17.5 EJB Client Responsibilities
    //      A transactional client can not change principal association within transaction.
    //      A session bean client must not change the principal association for the duration of the communication.
    //      If transactional requests within a single transaction arrive from multiple clients all must be associated
    //        with the same security context.

    // 17.6.3 - Security Mechanisms
    // 17.6.4 - Passing Principals on EJB Calls
    // 17.6.6 - Secure Access to Resource Managers
    // 17.6.7 - Principal Mapping
    // 17.6.9 - Runtime Security Enforcement
    // 17.6.10 - Audit Trail
}

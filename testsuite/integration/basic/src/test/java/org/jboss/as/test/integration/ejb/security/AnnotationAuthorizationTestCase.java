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
package org.jboss.as.test.integration.ejb.security;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.security.authorization.DenyAllOverrideBean;
import org.jboss.as.test.integration.ejb.security.authorization.PermitAllOverrideBean;
import org.jboss.as.test.integration.ejb.security.authorization.RolesAllowedOverrideBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case to test the general authorization requirements for annotated beans, more specific requirements such as RunAs
 * handling will be in their own test case.
 * <p/>
 * EJB 3.1 Section 17.3.2.1
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
public class AnnotationAuthorizationTestCase extends SecurityTest {

    private static final Logger log = Logger.getLogger(AnnotationAuthorizationTestCase.class.getName());

    @Deployment
    public static Archive<?> runAsDeployment() {
        // FIXME hack to get things prepared before the deployment happens
        try {
            // create required security domains
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }

        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addPackage(RolesAllowedOverrideBean.class.getPackage()).addClass(Util.class)
                .addClass(AnnotationAuthorizationTestCase.class).addClass(SecurityTest.class)
                .addAsResource("ejb3/security/users.properties", "users.properties")
                .addAsResource("ejb3/security/roles.properties", "roles.properties")
                .addAsWebInfResource("ejb3/security/jboss-web.xml", "jboss-web.xml")
                .addAsManifestResource("web-secure-programmatic-login.war/MANIFEST.MF", "MANIFEST.MF");
        log.info(war.toString(true));
        return war;
    }

    @EJB(mappedName = "java:global/ejb3security/RolesAllowedOverrideBean")
    private RolesAllowedOverrideBean rolesAllowedOverridenBean;

    /*
     * Test overrides within a bean annotated @RolesAllowed at bean level.
     */

    @Test
    public void testRolesAllowedOverriden_NoUser() throws Exception {
        try {
            rolesAllowedOverridenBean.defaultEcho("1");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        try {
            rolesAllowedOverridenBean.denyAllEcho("2");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        String response = rolesAllowedOverridenBean.permitAllEcho("3");
        assertEquals("3", response);

        try {
            rolesAllowedOverridenBean.role2Echo("4");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }
    }

    @Test
    public void testRolesAllowedOverriden_User1() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();

        try {

            String response = rolesAllowedOverridenBean.defaultEcho("1");
            assertEquals("1", response);

            try {
                rolesAllowedOverridenBean.denyAllEcho("2");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            response = rolesAllowedOverridenBean.permitAllEcho("3");
            assertEquals("3", response);

            try {
                rolesAllowedOverridenBean.role2Echo("4");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testRolesAllowedOverriden_User2() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user2", "password2");
        lc.login();

        try {
            try {
                rolesAllowedOverridenBean.defaultEcho("1");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            try {
                rolesAllowedOverridenBean.denyAllEcho("2");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            String response = rolesAllowedOverridenBean.permitAllEcho("3");
            assertEquals("3", response);

            response = rolesAllowedOverridenBean.role2Echo("4");
            assertEquals("4", response);

        } finally {
            lc.logout();
        }
    }

    /*
     * Test overrides of bean annotated at bean level with @PermitAll
     */

    @EJB(mappedName = "java:global/ejb3security/PermitAllOverrideBean")
    private PermitAllOverrideBean permitAllOverrideBean;

    @Test
    public void testPermitAllOverride_NoUser() throws Exception {
        String response = permitAllOverrideBean.defaultEcho("1");
        assertEquals("1", response);

        try {
            permitAllOverrideBean.denyAllEcho("2");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        try {
            permitAllOverrideBean.role1Echo("3");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }
    }

    @Test
    public void testPermitAllOverride_User1() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();

        try {
            String response = permitAllOverrideBean.defaultEcho("1");
            assertEquals("1", response);

            try {
                permitAllOverrideBean.denyAllEcho("2");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            response = permitAllOverrideBean.role1Echo("3");
            assertEquals("3", response);
        } finally {
            lc.logout();
        }
    }

    /*
     * Test overrides of ben annotated at bean level with @DenyAll
     */

    @EJB(mappedName = "java:global/ejb3security/DenyAllOverrideBean")
    private DenyAllOverrideBean denyAllOverrideBean;

    @Test
    public void testDenyAllOverride_NoUser() throws Exception {
        try {
            denyAllOverrideBean.defaultEcho("1");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        String response = denyAllOverrideBean.permitAllEcho("2");
        assertEquals("2", response);

        try {
            denyAllOverrideBean.role1Echo("3");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }
    }

    @Test
    public void testDenyAllOverride_User1() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();

        try {
            try {
                denyAllOverrideBean.defaultEcho("1");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            String response = denyAllOverrideBean.permitAllEcho("2");
            assertEquals("2", response);

            response = denyAllOverrideBean.role1Echo("3");
            assertEquals("3", response);
        } finally {
            lc.logout();
        }
    }

}

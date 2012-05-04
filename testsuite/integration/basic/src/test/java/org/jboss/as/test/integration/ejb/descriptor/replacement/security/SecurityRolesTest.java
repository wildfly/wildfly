/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.security;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import junit.framework.Assert;
import org.jboss.as.test.shared.integration.ejb.security.Util;

/**
 *
 * @author rhatlapa
 */
public class SecurityRolesTest {

    /**
     * tests method permissions for methods in RoleProtectedBean for user1 with role1
     *
     * @param ctx
     * @throws Exception
     */
    protected void testSecurityRolesUser1(InitialContext ctx) throws Exception {
        final RoleProtectedBean bean = (RoleProtectedBean) ctx.lookup("java:module/RoleProtectedBean");
        LoginContext lc = Util.getCLMLoginContext("user1", "password");
        lc.login();
        String response;
        try {

            Assert.assertTrue("User should be in role1", bean.isInRole("role1"));
            Assert.assertFalse("User is expected not being in role2", bean.isInRole("role2"));

            try {
                response = bean.defaultEcho("1");
                Assert.assertEquals("1", response);
            } catch (EJBAccessException ex) {
                Assert.fail("Not expected thrown exception for defaultEcho");
            }
            try {
                bean.denyAllEcho("2");
                Assert.fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            try {
                response = bean.permitAllEcho("3");
                Assert.assertEquals("3", response);
            } catch (EJBAccessException ex) {
                Assert.fail("PermitAllEcho should be allowed for all security roles");
            }            

        } finally {
            lc.logout();
        }
    }

    /**
     * tests method permissions for methods in RoleProtectedBean for user2 with role2
     *
     * @param ctx
     * @throws Exception
     */
    protected void testSecurityRolesUser2(InitialContext ctx) throws Exception {
        final RoleProtectedBean bean = (RoleProtectedBean) ctx.lookup("java:module/RoleProtectedBean");
        LoginContext lc = Util.getCLMLoginContext("user2", "password");
        lc.login();
        String response;
        try {
            Assert.assertTrue("User should be in role2", bean.isInRole("role2"));
            Assert.assertFalse("User is expected not being in role1", bean.isInRole("role1"));

            try {
                response = bean.defaultEcho("1");
                Assert.assertEquals("1", response);
            } catch (EJBAccessException ex) {
                Assert.fail("Not expected thrown exception for defaultEcho");
            }

            try {
                bean.denyAllEcho("2");
                Assert.fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            try {
                response = bean.permitAllEcho("3");
                Assert.assertEquals("3", response);
            } catch (EJBAccessException ex) {
                Assert.fail("PermitAllEcho should be allowed for all security roles");
            }

            try {
                response = bean.role2Echo("4");
                Assert.assertEquals("user2 should have permission to access method role2Echo", "4", response);
            } catch (EJBAccessException ex) {
                Assert.fail("role2Echo should be allowed for security role role2");
            }
        } finally {
            lc.logout();
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.security;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TestCase for testing if only jboss-spec descriptor is deployed
 *
 * @author rhatlapa
 */
@RunWith(Arquillian.class)
@ServerSetup({EjbSecurityDomainSetup.class})
public class SecurityRolesJbossSpecTestCase extends SecurityRolesTest {

    private static final String DEPLOYMENT_JBOSS_SPEC_ONLY = "jboss-spec";

    /**
     * deploys only with JBoss specific descriptor
     *
     * @return
     */
    @Deployment(name = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class,
                "ejb-descriptor-configuration-test-jboss-spec.jar").
                addPackage(RoleProtectedBean.class.getPackage()).
                addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class).
                addClass(org.jboss.as.test.shared.integration.ejb.security.Util.class).
                addAsResource(SecurityRolesTest.class.getPackage(), "users.properties", "users.properties").
                addAsResource(SecurityRolesTest.class.getPackage(), "roles.properties", "roles.properties").
                addAsManifestResource(SecurityRolesTest.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testSecurityRolesUser1JbossSpec(@ArquillianResource InitialContext ctx) throws Exception {
        testSecurityRolesUser1(ctx);
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testSecurityRolesUser2JbossSpec(@ArquillianResource InitialContext ctx) throws Exception {
        testSecurityRolesUser2(ctx);
    }
    
    /**
     * tests method permissions for method role2Echo in RoleProtectedBean for user1 with role1
     *
     * @param ctx
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testAccessToRole2EchoNotAllowedForUser1(@ArquillianResource InitialContext ctx) throws Exception {
        final RoleProtectedBean bean = (RoleProtectedBean) ctx.lookup("java:module/RoleProtectedBean");
        LoginContext lc = Util.getCLMLoginContext("user1", "password");
        lc.login();
        String response;
        try {
            Assert.assertTrue("User should be in role1", bean.isInRole("role1"));
            Assert.assertFalse("User is expected not being in role2", bean.isInRole("role2"));            

            try {
                response = bean.role2Echo("4");
                Assert.fail("user1 should not have permission to access method role2Echo => EJBAccessException expected");
            } catch (EJBAccessException ex) {
                // ignore
            }

        } finally {
            lc.logout();
        }
    }
}

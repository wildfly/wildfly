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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TestCase for testing security roles if both descriptors are deployed (ejb-spec and jboss-spec)
 *
 * @author rhatlapa
 */
@RunWith(Arquillian.class)
@ServerSetup({EjbSecurityDomainSetup.class})
public class SecurityRolesWithJbossSpecRedefinitionTestCase extends SecurityRolesTest {

    private static final String DEPLOYMENT_WITH_REDEFINITION = "ejb3-specVsJboss-spec";

    /**
     * deploys with both EJB specific descriptor and JBoss specific descriptor
     *
     * @return
     */
    @Deployment(name = DEPLOYMENT_WITH_REDEFINITION)
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class,
                "ejb-descriptor-configuration-test.jar").
                addPackage(RoleProtectedBean.class.getPackage()).
                addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class).
                addClass(org.jboss.as.test.shared.integration.ejb.security.Util.class).
                addAsResource(SecurityRolesTest.class.getPackage(), "users.properties", "users.properties").
                addAsResource(SecurityRolesTest.class.getPackage(), "roles.properties", "roles.properties").
                addAsManifestResource(SecurityRolesTest.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml").
                addAsManifestResource(SecurityRolesTest.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Ignore("JBPAPP-8624 and AS7-4417")
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
    public void testSecurityRolesUser1WithJbossSpecRedefinition(@ArquillianResource InitialContext ctx) throws Exception {
        testSecurityRolesUser1(ctx);
    }

    @Ignore("JBPAPP-8624 and AS7-4417")
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
    public void testSecurityRolesUser2WithJbossSpecRedefinition(@ArquillianResource InitialContext ctx) throws Exception {
        testSecurityRolesUser2(ctx);
    }
    
     /**
     * tests method permissions for method role2Echo in RoleProtectedBean for user1 with role1
     *
     * @param ctx
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
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
                Assert.assertEquals("user1 should have permission to access method role2Echo thanks "
                        + "to merge of permissions from ejb-jar and jboss-spec descriptors", "4", response);
            } catch (EJBAccessException ex) {
                Assert.fail("user1 should have permission to access method role2Echo thanks "
                        + "to merge of permissions from ejb-jar and jboss-spec descriptors");
            }

        } finally {
            lc.logout();
        }
    }
}

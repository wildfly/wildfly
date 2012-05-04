/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.interceptors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rhatlapa
 */
@RunWith(Arquillian.class)
public class InterceptorWithJbossSpecRedefinitionTestCase extends InterceptorTests {

    private static final String DEPLOYMENT_WITH_REDEFINITION = "ejb3-specVsJboss-spec";

    @Deployment(name = DEPLOYMENT_WITH_REDEFINITION)
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class,
                "ejb-descriptor-configuration-test.jar").addPackage(InterceptorWithJbossSpecRedefinitionTestCase.class.
                getPackage()).addAsManifestResource(InterceptorWithJbossSpecRedefinitionTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml").
                addAsManifestResource(InterceptorWithJbossSpecRedefinitionTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    /**
     * tests interceptor if both descriptors (ejb-jar and jboss-ejb3) are deployed in the module
     * @param ctx
     * @throws NamingException 
     */
    @Ignore("JBPAPP-8517")
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
    public void testInterceptorWithJbossSpecRedefinition(@ArquillianResource InitialContext ctx) throws NamingException {
        testInterceptor(ctx, "Hello JbossSpecInterceptedEjbIntercepted");
    }
}

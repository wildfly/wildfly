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
import org.jboss.as.test.integration.ejb.descriptor.replacement.SimpleBeanTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rhatlapa
 */
@RunWith(Arquillian.class)
public class InterceptorJbossSpecTestCase extends InterceptorTests {

    private static final String DEPLOYMENT_JBOSS_SPEC_ONLY = "jboss-spec";

    @Deployment(name = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public static Archive<?> deploymentJbossSpec() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class,
                "ejb-descriptor-configuration-test-jboss-spec.jar").
                addPackage(InterceptorJbossSpecTestCase.class.getPackage()).
                addAsManifestResource(InterceptorJbossSpecTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    /**
     * Tests interceptor behavior if only jboss-spec descriptor is deployed with the module
     * @param ctx
     * @throws NamingException 
     */
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testInterceptorJbossSpec(@ArquillianResource InitialContext ctx) throws NamingException {
        testInterceptor(ctx, "Hello JbossSpecIntercepted");
    }
}

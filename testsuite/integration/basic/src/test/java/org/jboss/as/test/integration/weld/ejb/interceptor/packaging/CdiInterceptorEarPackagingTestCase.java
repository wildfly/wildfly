/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.packaging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.packaging.war.namingcontext.EjbInterface;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * AS7-1032
 *
 * Tests the {@link org.jboss.as.weld.ejb.Jsr299BindingsInterceptor} is only applied to appropriate EJB's
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class CdiInterceptorEarPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {
         EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "CdiInterceptorPackaging.ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClass(EjbInterface.class);
        ear.addAsLibrary(lib);

        JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "cdiJar.jar");
        jar1.addClasses(CdiInterceptorEarPackagingTestCase.class, CdiInterceptorBinding.class, CdiInterceptor.class, CdiEjb.class);
        jar1.add(new StringAsset("<beans><interceptors><class>"+ CdiInterceptor.class.getName() + "</class></interceptors></beans>"), "META-INF/beans.xml");
        ear.addAsModule(jar1);

        JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "nonCdiJar.jar");
        jar2.addClasses(NonCdiEjb.class);
        ear.addAsModule(jar2);
        return ear;
    }

    @Test
    public void testCdiInterceptorApplied() throws NamingException {
        CdiEjb cdiEjb = (CdiEjb) new InitialContext().lookup("java:app/cdiJar/CdiEjb");
        Assert.assertEquals("Hello World", cdiEjb.sayHello());
    }

    @Test
    public void testCdiInterceptorNotApplied() throws NamingException {
        NonCdiEjb cdiEjb = (NonCdiEjb) new InitialContext().lookup("java:app/nonCdiJar/NonCdiEjb");
        Assert.assertEquals("Hello", cdiEjb.sayHello());
    }
}

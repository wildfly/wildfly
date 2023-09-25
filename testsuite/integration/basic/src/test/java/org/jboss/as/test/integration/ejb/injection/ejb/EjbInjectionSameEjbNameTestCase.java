/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that @Ejb injection works when two beans have the same name in different modules.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbInjectionSameEjbNameTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,"testinject.ear");
        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class,"lib.jar");
        lib.addClass(BeanInterface.class);
        ear.addAsLibraries(lib);

        final JavaArchive b1 = ShrinkWrap.create(JavaArchive.class,"b1.jar");
        b1.addClass(Bean1.class);
        ear.addAsModule(b1);
        final JavaArchive b2 = ShrinkWrap.create(JavaArchive.class,"b2.jar");
        b2.addClass(Bean2.class);
        ear.addAsModule(b2);
        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(EjbInjectionSameEjbNameTestCase.class, InjectingBean.class);
        ear.addAsModule(main);
        return ear;

    }


    @Test
    public void testCorrectEjbInjected() throws NamingException {
        InitialContext ctx = new InitialContext();
        InjectingBean bean = (InjectingBean)ctx.lookup("java:module/" + InjectingBean.class.getSimpleName());
        Assert.assertEquals("Bean1", bean.getBean1Name());
        Assert.assertEquals("Bean2", bean.getBean2Name());
    }


    @Test
    public void testEjbAnnotationOnClass() throws NamingException {
        InitialContext ctx = new InitialContext();
        InjectingBean bean = (InjectingBean)ctx.lookup("java:module/" + InjectingBean.class.getSimpleName());
        Assert.assertEquals("Bean1", bean.getJndiEjbName());
    }

}

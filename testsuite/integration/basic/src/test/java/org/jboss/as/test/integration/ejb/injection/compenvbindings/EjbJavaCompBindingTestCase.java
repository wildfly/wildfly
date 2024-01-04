/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.compenvbindings;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that EJB's resource injections in an ear correctly bind to the EJB's java:comp namespace
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbJavaCompBindingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,"testinject.ear");

        final JavaArchive b1 = ShrinkWrap.create(JavaArchive.class,"b1.jar");
        b1.addPackage(EjbJavaCompBindingTestCase.class.getPackage());
        ear.addAsModule(b1);
        return ear;

    }


    @Test
    public void testCorrectEjbInjected() throws NamingException {
        InitialContext ctx = new InitialContext();
        InjectingBean bean = (InjectingBean)ctx.lookup("java:module/" + InjectingBean.class.getSimpleName());
        Assert.assertNotNull(bean.getBean());
        Assert.assertNotNull(bean.getBeanViaDirectLookup());
    }



}

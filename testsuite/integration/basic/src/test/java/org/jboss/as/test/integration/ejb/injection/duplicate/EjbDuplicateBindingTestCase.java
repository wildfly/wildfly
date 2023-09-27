/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.duplicate;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the same EJB can be bound twice to the same java:global namespace with an env entry
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbDuplicateBindingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,"testglobal.ear");

        final JavaArchive b1 = ShrinkWrap.create(JavaArchive.class,"b1.jar");
        b1.addPackage(EjbDuplicateBindingTestCase.class.getPackage());
        ear.addAsModule(b1);
        return ear;

    }


    @Test
    public void testCorrectEjbInjected() throws NamingException {
        InitialContext ctx = new InitialContext();
        InjectingBean bean = (InjectingBean)ctx.lookup("java:module/" + InjectingBean.class.getSimpleName());
        Assert.assertNotNull(bean.getBean());
        Assert.assertNotNull(bean.lookupGlobalBean());
        InjectingBean2 bean2 = (InjectingBean2)ctx.lookup("java:module/" + InjectingBean2.class.getSimpleName());
        Assert.assertNotNull(bean2.getBean());
        Assert.assertNotNull(bean2.lookupGlobalBean());
    }



}

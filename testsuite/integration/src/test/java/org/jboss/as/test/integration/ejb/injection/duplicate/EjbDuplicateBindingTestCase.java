/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

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
package org.jboss.as.test.integration.ejb.injection.ejbs;

import javax.naming.InitialContext;
import javax.naming.NamingException;

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

/**
 * Tests that @EJBs() injection works as expected
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbsInjectionTestCase {

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
        main.addClasses(EjbsInjectionTestCase.class, InjectingBean.class);
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



}

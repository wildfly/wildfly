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

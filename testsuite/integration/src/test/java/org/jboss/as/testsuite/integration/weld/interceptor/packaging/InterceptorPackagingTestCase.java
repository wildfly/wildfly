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
package org.jboss.as.testsuite.integration.weld.interceptor.packaging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 *
 * Tests that interceptors that are packaged in seperate jar files.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@Ignore("AS7-1181")
public class InterceptorPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "interceptortest.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mod1.jar");
        jar.addClasses(InterceptedBean.class, SimpleEjb.class, InterceptorPackagingTestCase.class);
        jar.addAsManifestResource(new StringAsset("<beans><interceptors><class>" + SimpleInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");
        ear.addAsModule(jar);

        jar = ShrinkWrap.create(JavaArchive.class, "mod2.jar");
        jar.addClasses(SimpleInterceptor.class, SimpleEjb2.class, Intercepted.class);
        jar.addAsManifestResource(new StringAsset("<beans><interceptors><class>" + SimpleInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");
        ear.addAsModule(jar);

        return ear;
    }

    @Inject
    private SimpleEjb simpleEjb;

    @Inject
    private SimpleEjb2 simpleEjb2;

    @Inject
    private InterceptedBean interceptedBean;

    @Test
    public void testInterceptorEnabled() {
        Assert.assertEquals("Hello World", simpleEjb2.sayHello());
        Assert.assertEquals("Hello World", interceptedBean.sayHello());
        Assert.assertEquals("Hello World", simpleEjb.sayHello());
    }


}

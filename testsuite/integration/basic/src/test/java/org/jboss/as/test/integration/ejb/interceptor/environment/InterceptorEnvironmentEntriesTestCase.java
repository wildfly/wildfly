/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.interceptor.environment;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that interceptor environment entries defined in ejb-jar are processed
 * <p/>
 * AS7-2776
 */
@RunWith(Arquillian.class)
public class InterceptorEnvironmentEntriesTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "interceptor-complexb-test.jar")
                .addPackage(InterceptorEnvironmentEntriesTestCase.class.getPackage())
                .addAsManifestResource(InterceptorEnvironmentEntriesTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testInjection() throws Exception {
        MySession2RemoteB test = (MySession2RemoteB) ctx.lookup("java:module/MySession2BeanB");
        boolean a = test.doit();
        Assert.assertEquals(false, a);
    }

    @Test
    public void testInjection2() throws Exception {
        MyTestRemoteB test = (MyTestRemoteB) ctx.lookup("java:module/MyTestB");
        boolean a = test.doit();
        Assert.assertEquals(true, a);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

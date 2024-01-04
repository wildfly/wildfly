/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.destroy;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that lifecycle interceptors are handed correctly,
 * as per the interceptors specification.
 *
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class PreDestroyInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class,"testpredestroy.jar");
        jar.addPackage(PreDestroyInterceptorTestCase.class.getPackage());
        jar.addAsManifestResource(PreDestroyInterceptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testPreDestroyInterceptor() throws NamingException {

        InitialContext ctx = new InitialContext();
        PreDestroySFSB bean = (PreDestroySFSB)ctx.lookup("java:module/" + PreDestroySFSB.class.getSimpleName());
        Assert.assertTrue(PreDestroySFSB.postConstructCalled);
        Assert.assertTrue(PreDestroyInterceptor.postConstruct);
        Assert.assertFalse("InvocationContext.getTarget() was null for post-construct interceptor", PreDestroyInterceptor.postConstructInvocationTargetNull);
        Assert.assertTrue(PreDestroyInterceptorDescriptor.postConstruct);
        Assert.assertFalse("InvocationContext.getTarget() was null for post-construct interceptor", PreDestroyInterceptorDescriptor.postConstructInvocationTargetNull);

        bean.remove();
        Assert.assertTrue(PreDestroySFSB.preDestroyCalled);
        Assert.assertTrue(PreDestroyInterceptor.preDestroy);
        Assert.assertFalse("InvocationContext.getTarget() was null for pre-destroy interceptor", PreDestroyInterceptor.preDestroyInvocationTargetNull);
        Assert.assertTrue(PreDestroyInterceptorDescriptor.preDestroy);
        Assert.assertFalse("InvocationContext.getTarget() was null for pre-destroy interceptor", PreDestroyInterceptorDescriptor.preDestroyInvocationTargetNull);
    }
}

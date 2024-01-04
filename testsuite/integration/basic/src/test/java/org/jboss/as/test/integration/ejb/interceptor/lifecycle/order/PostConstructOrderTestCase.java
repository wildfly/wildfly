/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class PostConstructOrderTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testlocal.war");
        war.addPackage(PostConstructOrderTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testPostConstructMethodOrder() throws NamingException {
        InitialContext ctx = new InitialContext();
        SFSBChild bean = (SFSBChild) ctx.lookup("java:module/" + SFSBChild.class.getSimpleName());
        bean.doStuff();
        Assert.assertTrue(SFSBParent.parentPostConstructCalled);
        Assert.assertTrue(SFSBChild.childPostConstructCalled);
        Assert.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assert.assertTrue(InterceptorChild.childPostConstructCalled);
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertTrue(SFSBParent.parentPostConstructCalled);
        Assertions.assertTrue(SFSBChild.childPostConstructCalled);
        Assertions.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assertions.assertTrue(InterceptorChild.childPostConstructCalled);
    }

}

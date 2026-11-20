/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.cdi.requestscope;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that an @Timeout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class CDIRequestScopeTimerServiceTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(CDIRequestScopeTimerServiceTestCase.class.getPackage());
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;

    }

    @Test
    public void testRequestScopeActive() throws NamingException {
        InitialContext ctx = new InitialContext();
        AnnotationTimerServiceBean bean = (AnnotationTimerServiceBean) ctx.lookup("java:module/" + AnnotationTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assertions.assertEquals(CdiBean.MESSAGE, AnnotationTimerServiceBean.awaitTimerCall());
    }


}

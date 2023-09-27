/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.view;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that an @Timeout method works when it is not part of any bean views
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ViewTimerServiceTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceView.war");
        war.addPackage(ViewTimerServiceTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testAnnotationTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        LocalInterface bean = (LocalInterface) ctx.lookup("java:module/" + AnnotationTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(AnnotationTimerServiceBean.awaitTimerCall());
    }

    @Test
    public void testTimedObjectTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        LocalInterface bean = (LocalInterface) ctx.lookup("java:module/" + TimedObjectTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());
    }


}

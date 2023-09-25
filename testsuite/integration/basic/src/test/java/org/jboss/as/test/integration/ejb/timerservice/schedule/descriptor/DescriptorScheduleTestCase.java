/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.schedule.descriptor;

import java.util.Date;

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
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DescriptorScheduleTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testDescriptorSchedule.war");
        war.addPackage(DescriptorScheduleTestCase.class.getPackage());
        war.addAsWebInfResource(DescriptorScheduleTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;

    }

    @Test
    public void testDescriptorBasedSchedule() throws NamingException {
        InitialContext ctx = new InitialContext();
        DescriptorScheduleBean bean = (DescriptorScheduleBean) ctx.lookup("java:module/" + DescriptorScheduleBean.class.getSimpleName());
        Assert.assertTrue(DescriptorScheduleBean.awaitTimer());
        Assert.assertEquals("INFO", DescriptorScheduleBean.getTimerInfo());
        Assert.assertEquals(new Date(90, 0, 1, 0, 0, 0), DescriptorScheduleBean.getStart());
    }

}

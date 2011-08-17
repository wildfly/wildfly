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
package org.jboss.as.testsuite.timerservice.schedule.descriptor;

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
import java.util.Date;

/**
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DescriptorScheduleTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,"testDescriptorSchedule.war");
        war.addPackage(DescriptorScheduleTestCase.class.getPackage());
        war.addAsWebInfResource("schedule-descriptor-ejb-jar.xml", "ejb-jar.xml");
        return war;

    }

    @Test
    public void testDescriptorBasedSchedule() throws NamingException {
        InitialContext ctx = new InitialContext();
        DescriptorScheduleBean bean = (DescriptorScheduleBean)ctx.lookup("java:module/" + DescriptorScheduleBean.class.getSimpleName());
        Assert.assertTrue(DescriptorScheduleBean.awaitTimer());
        Assert.assertEquals("INFO", DescriptorScheduleBean.getTimerInfo());
        Assert.assertEquals(new Date(90,0,1,0,0,0), DescriptorScheduleBean.getStart());
    }



}

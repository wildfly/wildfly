/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.concurrent.scheduled;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for managed executor's hung tasks termination feature.
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
public class ExceptionInScheduleTestCase {

    @EJB(lookup = "java:module/ExecFlawedTaskBean!org.jboss.as.test.integration.ee.concurrent.scheduled.ExecNumber")
    private ExecNumber execNumber;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, ExceptionInScheduleTestCase.class.getSimpleName() + ".jar")
                .addClasses(ExecFlawedTaskBean.class, ExecNumber.class);
    }

    @Test
    public void testExecutionCount() throws Exception {
        execNumber.start();
        Thread.currentThread().sleep(2000);
        execNumber.cease();
        Assert.assertEquals(execNumber.expected(), execNumber.actual());
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent.scheduled;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test case for managed executor's hung tasks termination feature.
 *
 * @author baranowb
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(execNumber.expected(), execNumber.actual());
    }

}

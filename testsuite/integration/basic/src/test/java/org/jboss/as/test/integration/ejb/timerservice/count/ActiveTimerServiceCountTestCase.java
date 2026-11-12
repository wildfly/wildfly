/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.count;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.ejb.Timer;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Testcase for the {@link jakarta.ejb.TimerService#getAllTimers()} API introduced in EJB 3.2 spec
 *
 * @author: Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class ActiveTimerServiceCountTestCase {

    private static final String APP_NAME = "ejb-3.2-active-timers";
    private static final String MODULE_ONE_NAME = "ejb-3.2-active-timers-one";
    private static final String MODULE_TWO_NAME = "ejb-3.2-active-timers-two";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJarOne = ShrinkWrap.create(JavaArchive.class, MODULE_ONE_NAME + ".jar");
        ejbJarOne.addClasses(ActiveTimerServiceCountTestCase.class, SimpleTimerBean.class, OtherTimerBeanInSameModule.class, StatefulBean.class);

        final JavaArchive ejbJarTwo = ShrinkWrap.create(JavaArchive.class, MODULE_TWO_NAME + ".jar");
        ejbJarTwo.addClasses(TimerBeanInOtherModule.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        ear.addAsModules(ejbJarOne, ejbJarTwo);

        return ear;
    }

    /**
     * Tests that the {@link jakarta.ejb.TimerService#getAllTimers()} API introduced in EJB 3.2 works as expected
     *
     * @throws Exception
     */
    @Test
    public void testActiveTimersCountInEJBModule() throws Exception {
        final SimpleTimerBean timerBean = InitialContext.doLookup("java:module/" + SimpleTimerBean.class.getSimpleName() + "!" + SimpleTimerBean.class.getName());
        final String infoOne = "gangnam style";
        timerBean.createTimerForNextDay(false, infoOne);
        final String infoTwo = "PSY";
        timerBean.createTimerForNextDay(true, infoTwo);

        final OtherTimerBeanInSameModule otherTimerBean = InitialContext.doLookup("java:module/" + OtherTimerBeanInSameModule.class.getSimpleName() + "!" + OtherTimerBeanInSameModule.class.getName());
        final String infoThree = "hello world!";
        otherTimerBean.createTimerForNextDay(false, infoThree);

        final TimerBeanInOtherModule timerBeanInOtherModule = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_TWO_NAME + "/" + TimerBeanInOtherModule.class.getSimpleName() + "!" + TimerBeanInOtherModule.class.getName());
        final String infoForTimerBeanInOtherModule = "irrelevant";
        timerBeanInOtherModule.createTimerForNextDay(false, infoForTimerBeanInOtherModule);

        final Collection<Timer> activeTimers = new ArrayList<>(timerBean.getAllActiveTimersInEJBModule());

        // now start testing
        Assertions.assertFalse(activeTimers.isEmpty(), "No active timers found in EJB module " + MODULE_ONE_NAME);

        // now make sure that the active timers are indeed the one we expected
        Assertions.assertTrue(this.removeScheduleOneTimerOfSimpleTimerBean(activeTimers), "@Schedule timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers");
        Assertions.assertTrue(this.removeScheduleTwoTimerOfSimpleTimerBean(activeTimers), "@Schedule timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers");
        Assertions.assertTrue(this.removeSheduleOneTimerOfOtherTimerBean(activeTimers), "@Schedule timer on " + OtherTimerBeanInSameModule.class.getSimpleName() + " not found in active timers");
        Assertions.assertTrue(this.removeProgramaticTimer(activeTimers, infoOne, false), "Programmatic timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers");
        Assertions.assertTrue(this.removeProgramaticTimer(activeTimers, infoTwo, true), "Programmatic timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers");
        Assertions.assertTrue(this.removeProgramaticTimer(activeTimers, infoThree, false), "Programmatic timer on " + OtherTimerBeanInSameModule.class.getSimpleName() + " not found in active timers");

        // make sure there isn't an unexpected timer in the active timers
        for (final Timer remainingTimer : activeTimers) {
            Assertions.assertNotEquals("Unexpectedly found a timer from other EJB module " + MODULE_TWO_NAME, TimerBeanInOtherModule.SCHEDULE_ONE_INFO.equals(remainingTimer.getInfo()));
            Assertions.assertNotEquals("Unexpectedly found a timer from other EJB module " + MODULE_TWO_NAME, infoForTimerBeanInOtherModule.equals(remainingTimer.getInfo()));
        }

        // Now fetch the same info, this time from the SFSB. We expect the same number of active timers in the module.
        final StatefulBean statefulBean = InitialContext.doLookup("java:module/" + StatefulBean.class.getSimpleName() + "!" + StatefulBean.class.getName());
        final Collection<Timer> activeTimersReturnedFromSFSB = new ArrayList<>(statefulBean.getAllActiveTimersInEJBModule());

        Assertions.assertFalse(activeTimersReturnedFromSFSB.isEmpty(), "No active timers found in EJB module " + MODULE_ONE_NAME + " when queried from a stateful bean");

        // now make sure that the active timers are indeed the one we expected
        Assertions.assertTrue(this.removeScheduleOneTimerOfSimpleTimerBean(activeTimersReturnedFromSFSB), "@Schedule timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers when queried from a stateful bean");
        Assertions.assertTrue(this.removeScheduleTwoTimerOfSimpleTimerBean(activeTimersReturnedFromSFSB), "@Schedule timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers when queried from a stateful bean");
        Assertions.assertTrue(this.removeSheduleOneTimerOfOtherTimerBean(activeTimersReturnedFromSFSB), "@Schedule timer on " + OtherTimerBeanInSameModule.class.getSimpleName() + " not found in active timers when queried from a stateful bean");
        Assertions.assertTrue(this.removeProgramaticTimer(activeTimersReturnedFromSFSB, infoOne, false), "Programmatic timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers when queried from a stateful bean");
        Assertions.assertTrue(this.removeProgramaticTimer(activeTimersReturnedFromSFSB, infoTwo, true), "Programmatic timer on " + SimpleTimerBean.class.getSimpleName() + " not found in active timers when queried from a stateful bean");
        Assertions.assertTrue(this.removeProgramaticTimer(activeTimersReturnedFromSFSB, infoThree, false), "Programmatic timer on " + OtherTimerBeanInSameModule.class.getSimpleName() + " not found in active timers when queried from a stateful bean");

        // make sure there isn't an unexpected timer in the active timers
        for (final Timer remainingTimer : activeTimersReturnedFromSFSB) {
            Assertions.assertNotEquals("Unexpectedly found a timer from other EJB module " + MODULE_TWO_NAME + " when queried from a stateful bean", TimerBeanInOtherModule.SCHEDULE_ONE_INFO.equals(remainingTimer.getInfo()));
            Assertions.assertNotEquals("Unexpectedly found a timer from other EJB module " + MODULE_TWO_NAME + " when queried from a stateful bean", infoForTimerBeanInOtherModule.equals(remainingTimer.getInfo()));
        }

    }



    private boolean removeScheduleOneTimerOfSimpleTimerBean(final Collection<Timer> timers) {
        for (final Timer activeTimer : timers) {
            // see if it's @Schedule one in SimpleTimerBean
            final Serializable info = activeTimer.getInfo();
            if (SimpleTimerBean.SCHEDULE_ONE_INFO.equals(info)) {
                Assertions.assertTrue(activeTimer.isCalendarTimer(), "Unexpected timer type of timer on " + SimpleTimerBean.class.getSimpleName());
                Assertions.assertFalse(activeTimer.isPersistent(), "Unexpected persistence type of timer on " + SimpleTimerBean.class.getSimpleName());
                // remove this matched timer from the collection
                return timers.remove(activeTimer);
            }
        }
        return false;
    }

    private boolean removeScheduleTwoTimerOfSimpleTimerBean(final Collection<Timer> timers) {
        for (final Timer activeTimer : timers) {
            // see if it's @Schedule two in SimpleTimerBean
            final Serializable info = activeTimer.getInfo();
            if (SimpleTimerBean.SCHEDULE_TWO_INFO.equals(info)) {
                Assertions.assertTrue(activeTimer.isCalendarTimer(), "Unexpected timer type of timer on " + SimpleTimerBean.class.getSimpleName());
                Assertions.assertTrue(activeTimer.isPersistent(), "Unexpected persistence type of timer on " + SimpleTimerBean.class.getSimpleName());
                // remove this matched timer from the collection
                return timers.remove(activeTimer);
            }
        }
        return false;
    }

    private boolean removeSheduleOneTimerOfOtherTimerBean(final Collection<Timer> timers) {
        for (final Timer activeTimer : timers) {
            // see if it's @Schedule one in OtherTimerBeanInSameModule
            final Serializable info = activeTimer.getInfo();
            if (OtherTimerBeanInSameModule.SCHEDULE_ONE_INFO.equals(info)) {
                Assertions.assertTrue(activeTimer.isCalendarTimer(), "Unexpected timer type of timer on " + OtherTimerBeanInSameModule.class.getSimpleName());
                Assertions.assertFalse(activeTimer.isPersistent(), "Unexpected persistence type of timer on " + OtherTimerBeanInSameModule.class.getSimpleName());
                // remove this matched timer from the collection
                return timers.remove(activeTimer);
            }
        }
        return false;
    }

    private boolean removeProgramaticTimer(final Collection<Timer> timers, final String timerInfo, final boolean persistent) {
        for (final Timer activeTimer : timers) {
            // see if it's the programatic timer we are looking for
            final Serializable info = activeTimer.getInfo();
            if (timerInfo.equals(info)) {
                Assertions.assertFalse(activeTimer.isCalendarTimer(), "Unexpected timer type of timer");
                Assertions.assertEquals(persistent, activeTimer.isPersistent(), "Unexpected persistence type of timer");
                // remove this matched timer from the collection
                return timers.remove(activeTimer);
            }
        }
        return false;
    }
}

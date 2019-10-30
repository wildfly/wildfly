package org.jboss.as.test.integration.ejb.timerservice.cancelation;

import java.util.concurrent.TimeUnit;

import javax.ejb.TimerHandle;
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
 * Test that timer cancellation works as expected, even when there is a race between the timeout method and the
 * cancellation itself
 *
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServiceCancellationTestCase {
    // countlatchdown waiting time
    private static int TIMER_CALL_WAITING_S = 30;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "timerCancelTest.jar");
        jar.addPackage(TimerServiceCancellationTestCase.class.getPackage());
        return jar;
    }


    @Test
    public void testCancelSimpleWhileTimeoutActive() throws NamingException, InterruptedException {
        InitialContext ctx = new InitialContext();
        SimpleTimerServiceBean bean = (SimpleTimerServiceBean)ctx.lookup("java:module/" + SimpleTimerServiceBean.class.getSimpleName());
        TimerHandle handle = bean.createTimer();
        Assert.assertTrue(bean.getTimerEntry().await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS));
        //now the timeout is in progress cancel the timer
        handle.getTimer().cancel();
        bean.getTimerExit().countDown();
        Assert.assertFalse(SimpleTimerServiceBean.quickAwaitTimerCall());
        Assert.assertEquals(0, bean.getTimerCount());

    }


    @Test
    public void testCancelCalendarWhileTimeoutActive() throws NamingException, InterruptedException {
        InitialContext ctx = new InitialContext();
        CalendarTimerServiceBean bean = (CalendarTimerServiceBean)ctx.lookup("java:module/" + CalendarTimerServiceBean.class.getSimpleName());
        TimerHandle handle = bean.createTimer();
        Assert.assertTrue(bean.getTimerEntry().await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS));
        //now the timeout is in progress cancel the timer
        handle.getTimer().cancel();
        bean.getTimerExit().countDown();
        Assert.assertFalse(CalendarTimerServiceBean.quickAwaitTimerCall());
        Assert.assertEquals(0, bean.getTimerCount());
    }

}

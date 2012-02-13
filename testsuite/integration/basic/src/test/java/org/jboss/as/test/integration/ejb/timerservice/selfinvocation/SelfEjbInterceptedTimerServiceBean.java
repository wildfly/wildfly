package org.jboss.as.test.integration.ejb.timerservice.selfinvocation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.TimerService;
import javax.interceptor.Interceptors;

@Stateless
public class SelfEjbInterceptedTimerServiceBean {
    
    private static int TIMER_TIMEOUT_TIME_MS = 100;
    private static int TIMER_CALL_WAITING_MS = 2000;
    
    private static final CountDownLatch latch = new CountDownLatch(1);
    
    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    @Timeout
    public void timeout() {
        interceptedMethod();
    }

    @Interceptors({EjbInterceptor.class})
    public void interceptedMethod() {
        latch.countDown();
    }

    public static void awaitInterceptedMethod() throws InterruptedException {
        latch.await(TIMER_CALL_WAITING_MS, TimeUnit.MILLISECONDS);
    }
    
}

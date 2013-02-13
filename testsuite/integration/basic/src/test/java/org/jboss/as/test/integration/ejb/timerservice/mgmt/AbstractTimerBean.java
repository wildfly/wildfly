package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.jboss.logging.Logger;

public abstract class AbstractTimerBean implements SimpleFace {

    protected final Logger logger = Logger.getLogger(getClass());
    protected static int timerTicks = 0;
    protected static int delay = 5000;
    protected static Serializable info;
    protected static boolean persistent;
    protected static CountDownLatch latch;
    @Resource
    protected TimerService timerService;

    @Override
    public void clean() {
        for (Timer t : this.timerService.getTimers()) {
            t.cancel();
        }
        this.timerTicks = 0;
    }

    @Override
    public int getTimerTicks() {
        return this.timerTicks;
    }

    @Override
    public int getTimerCount() {
        return this.timerService.getTimers().size();
    }

    @Override
    public void waitOnTimeout() throws InterruptedException {
        this.latch = new CountDownLatch(1);
        this.latch.await(delay * 2, TimeUnit.MILLISECONDS);
    }

    @Timeout
    public void booom(Timer t) {
        this.timerTicks++;
        if (latch != null)
            latch.countDown();
    }

    @Override
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    @Override
    public void setInfo(Serializable info) {
        this.info = info;
    }

    @Override
    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public String getComparableTimerDetail() {
        return null;
    }

}

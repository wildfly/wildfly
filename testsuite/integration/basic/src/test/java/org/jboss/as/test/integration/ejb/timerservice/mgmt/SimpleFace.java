package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.io.Serializable;

public interface SimpleFace {

    public void clean();

    public void createTimer();

    public int getTimerCount();

    public int getTimerTicks();

    public void waitOnTimeout() throws InterruptedException;

    public void setPersistent(boolean persistent);

    public void setInfo(Serializable info);

    public void setDelay(int delay);

    public String getComparableTimerDetail();
}

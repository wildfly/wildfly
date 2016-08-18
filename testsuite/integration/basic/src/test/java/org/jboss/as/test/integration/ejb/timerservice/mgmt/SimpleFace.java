package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.io.Serializable;

public interface SimpleFace {

    void clean();

    void createTimer();

    int getTimerCount();

    int getTimerTicks();

    void waitOnTimeout() throws InterruptedException;

    void setPersistent(boolean persistent);

    void setInfo(Serializable info);

    void setDelay(int delay);

    String getComparableTimerDetail();
}

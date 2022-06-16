package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TimerConfig;

/**
 * @author: baranowb
 */
@Stateless
@Remote(SimpleFace.class)
public class IntervalTimerBean extends AbstractTimerBean {

    @Override
    public void createTimer() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(persistent);
        timerConfig.setInfo(info);
        super.timerService.createIntervalTimer(delay, delay, timerConfig);
    }
}

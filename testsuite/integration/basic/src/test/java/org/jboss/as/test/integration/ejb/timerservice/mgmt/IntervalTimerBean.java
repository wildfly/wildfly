package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TimerConfig;

/**
 * @author: baranowb
 */
@Stateless
@Remote(SimpleFace.class)
public class IntervalTimerBean extends AbstractTimerBean {

    @Override
    public void createTimer() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(super.persistent);
        timerConfig.setInfo(super.info);
        super.timerService.createIntervalTimer(super.delay, super.delay, timerConfig);
    }
}

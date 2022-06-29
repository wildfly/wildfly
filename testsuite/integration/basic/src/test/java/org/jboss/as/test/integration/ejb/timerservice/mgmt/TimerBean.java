package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author: baranowb
 */
@Stateless
@Remote(SimpleFace.class)
public class TimerBean extends AbstractTimerBean {

    @Override
    public void createTimer() {
        super.timerService.createTimer(delay, info);
    }

}

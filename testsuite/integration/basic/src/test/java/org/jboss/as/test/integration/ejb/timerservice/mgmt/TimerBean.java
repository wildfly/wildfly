package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import javax.ejb.Remote;
import javax.ejb.Stateless;

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

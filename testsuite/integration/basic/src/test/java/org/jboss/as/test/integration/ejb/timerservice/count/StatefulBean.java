package org.jboss.as.test.integration.ejb.timerservice.count;

import java.util.Collection;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateful;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author: Jaikiran Pai
 */
@Stateful(passivationCapable = false)
@LocalBean
public class StatefulBean {

    @Resource // we inject timerservice in a stateful bean so as to use (only) the TimerService.getAllTimers() method from the SFSB
    private TimerService timerService;

    public Collection<Timer> getAllActiveTimersInEJBModule() {
        return this.timerService.getAllTimers();
    }
}

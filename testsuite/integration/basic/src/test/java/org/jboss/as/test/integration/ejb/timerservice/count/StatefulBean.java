package org.jboss.as.test.integration.ejb.timerservice.count;

import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.ejb.Timer;
import javax.ejb.TimerService;

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

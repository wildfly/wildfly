package org.jboss.as.test.integration.ejb.timerservice.count;

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.Date;

/**
 * @author: Jaikiran Pai
 */
@Stateless
@LocalBean
public class TimerBeanInOtherModule {

    static final String SCHEDULE_ONE_INFO = "foo-bar";
    private static final Logger logger = Logger.getLogger(TimerBeanInOtherModule.class);

    @Resource
    private TimerService timerService;

    @Schedule(second="*", minute = "*", hour = "*", persistent = false, info = SCHEDULE_ONE_INFO)
    private void scheduleOne(final Timer timer) {
    }

    public void createTimerForNextDay(final boolean persistent, final String info) {
        this.timerService.createSingleActionTimer(new Date(System.currentTimeMillis() + (60 * 60 * 24 * 1000)), new TimerConfig(info, persistent));
        logger.trace("Created a timer persistent = " + persistent + " info = " + info);
    }
}

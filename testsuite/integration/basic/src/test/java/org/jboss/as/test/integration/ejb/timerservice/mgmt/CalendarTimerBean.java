package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;

/**
 * @author: baranowb
 */
@Stateless
@Remote(SimpleFace.class)
public class CalendarTimerBean extends AbstractTimerBean {

    @Override
    public void createTimer() {
        ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.second(getSeconds());
        scheduleExpression.hour("*");
        scheduleExpression.minute("*");
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(persistent);
        timerConfig.setInfo(info);
        super.timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    private String getSeconds() {
        final Calendar calendar = Calendar.getInstance();
        final int base = calendar.get(Calendar.SECOND);
        int current = base;
        List<Integer> list = new ArrayList<Integer>();
        final int boundary = base + 60;
        final int delay = AbstractTimerBean.delay / 1000;
        for (; current < boundary;) {
            list.add(current % 60);
            current += delay;
        }

        final int limit = list.size() - 1;
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < list.size(); index++) {
            stringBuilder.append(list.get(index));
            if (index < limit) {
                stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String getComparableTimerDetail() {
        for (Timer t : this.timerService.getTimers()) {
            return t.getSchedule().getSecond();
        }
        return null;
    }
}

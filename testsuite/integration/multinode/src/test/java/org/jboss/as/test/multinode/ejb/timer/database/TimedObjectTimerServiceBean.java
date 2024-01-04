/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.ejb.timer.database;

import org.jboss.as.test.shared.FileUtils;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TimedObject;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import java.util.Date;

/**
 * @author Stuart Douglas
 */
@Stateless
public class TimedObjectTimerServiceBean implements TimedObject, RemoteTimedBean {

    @Resource
    private TimerService timerService;

    private volatile boolean run = false;

    @EJB(lookup = "ejb:/testTimerServiceSimple/CollectionSingleton!org.jboss.as.test.multinode.ejb.timer.database.Collector")
    private Collector collector;

    private static final String NODE;

    static {
        NODE = FileUtils.readFile(TimedObjectTimerServiceBean.class.getClassLoader().getResource("node.txt"));
    }

    @Override
    public void scheduleTimer(long date, String info) {
        timerService.createSingleActionTimer(new Date(date), new TimerConfig(info, true));
    }

    @Override
    public boolean hasTimerRun() {
        return run;
    }

    @Override
    public void ejbTimeout(final Timer timer) {
        run = true;
        collector.timerRun(NODE, (String) timer.getInfo());

    }
}

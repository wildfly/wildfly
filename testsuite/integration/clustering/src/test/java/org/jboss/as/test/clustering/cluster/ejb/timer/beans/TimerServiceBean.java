/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.wildfly.clustering.group.Group;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.transaction.TransactionSynchronizationRegistry;

@Singleton
@Startup
@LocalBean
public class TimerServiceBean {

    private static Logger log = Logger.getLogger(TimerServiceBean.class);

    @Resource
    TimerService timerService;

    @Resource
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Resource(lookup = "java:jboss/clustering/group/ejb")
    Group group;

    private List<Timer> timers;

    @PostConstruct
    public void init() {
        timerService.getTimers().stream().forEach(Timer::cancel);
        timers = Collections.synchronizedList(new ArrayList<>());
    }

    @PreDestroy
    public void tearDown() {
        timerService.getTimers().stream().forEach(Timer::cancel);
        timers.clear();
    }

    public void createTimer(String value) {
        Timer timer = timerService.createSingleActionTimer(Duration.ofSeconds(5).toMillis(), new TimerConfig(value, true));
        log.debugf("create timer with info %s and timer %s", value, timer);
    }

    @Timeout
    public void timeout(Timer timer) {
        timers.add(timer);

        // WFLY-19891 Verify that this does not deadlock
        if (!timer.getInfo().equals(
                this.timerService.getTimers().stream().filter(timer::equals).findFirst().map(Timer::getInfo).orElse(null))) {
            throw new EJBException();
        }
    }

    public List<Timer> getTimers() {
        return timers;
    }
}
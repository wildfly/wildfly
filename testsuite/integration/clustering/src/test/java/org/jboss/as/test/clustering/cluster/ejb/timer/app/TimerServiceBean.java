/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.app;

import java.time.Instant;
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
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
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

    @PostConstruct
    public void init() {
        timerService.getTimers().stream().filter(timer -> ((TimerInfo) timer.getInfo()).getPostConstruct()).forEach(Timer::cancel);
    }

    @PreDestroy
    public void tearDown() {
        timerService.getTimers().stream().filter(timer -> ((TimerInfo) timer.getInfo()).getPreDestroy()).forEach(Timer::cancel);
    }

    public List<TimerInfo> getTimers() {
        return timerService.getTimers().stream().map(Timer::getInfo).map(TimerInfo.class::cast).toList();
    }

    public void createTimer(TimerInfo timerInfo) {
        Timer timer = timerService.createSingleActionTimer(timerInfo.getDuration(), new TimerConfig(timerInfo, true));
        log.debugf("create timer with info %s and timer %s", timerInfo, timer);
    }

    @Timeout
    public void timeout(Timer timer) {
        log.infof("timeout %s", timer.getInfo());
        TimerRecord timerRecord = new TimerRecord(group.getLocalMember().getName(), this.getClass().getName(), timer.getInfo(),
                timer.isPersistent(), Instant.now());
        // we avoid to send anything unless timeout completely commit tx
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void beforeCompletion() {

            }

            @Override
            public void afterCompletion(int status) {
                if (status != Status.STATUS_COMMITTED) {
                    return;
                }
                TimerWSEndpoint.send(timerRecord);
            }
        });

        // WFLY-19891 Verify that this does not deadlock
        if (!timer.getInfo().equals(
                this.timerService.getTimers().stream().filter(timer::equals).findFirst().map(Timer::getInfo).orElse(null))) {
            throw new EJBException();
        }
    }
}

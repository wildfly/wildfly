/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkException;

import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedAdminObject1;
import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedAdminObject1Impl;
import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedResourceAdapter1;
import org.jboss.jca.core.api.workmanager.DistributedWorkManager;

@Stateful(passivationCapable = false) //this is stateful so it maintains affinity, because standalone-ha.xml is used for the tests if a stateless bean is used invocations will go to other nodes
@Remote
public class DwmAdminObjectEjbImpl implements DwmAdminObjectEjb {

    @Resource(mappedName = "java:jboss/A1")
    private DistributedAdminObject1 dao;

    private DistributedWorkManager dwm;

    @PostConstruct
    public void initialize() {
        if (!(dao instanceof DistributedAdminObject1Impl)) {
            throw new IllegalStateException("DwmAdminObjectEjbImpl expects that its DistributedAdminObject1 will be of certain implemnetation");
        }
        DistributedAdminObject1Impl daoi = (DistributedAdminObject1Impl) dao;

        if (!(daoi.getResourceAdapter() instanceof DistributedResourceAdapter1)) {
            throw new IllegalStateException("DwmAdminObjectEjbImpl expects that its resource adapter will be distributable");
        }
        DistributedResourceAdapter1 dra = (DistributedResourceAdapter1) daoi.getResourceAdapter();
        dwm = dra.getDwm();
    }

    @Override
    public int getDoWorkAccepted() {
        return dwm.getStatistics().getDoWorkAccepted();
    }

    @Override
    public int getDoWorkRejected() {
        return dwm.getStatistics().getDoWorkRejected();
    }

    @Override
    public int getStartWorkAccepted() {
        return dwm.getStatistics().getStartWorkAccepted();
    }

    @Override
    public int getStartWorkRejected() {
        return dwm.getStatistics().getStartWorkRejected();
    }

    @Override
    public int getScheduleWorkAccepted() {
        return dwm.getStatistics().getScheduleWorkAccepted();
    }

    @Override
    public int getScheduleWorkRejected() {
        return dwm.getStatistics().getScheduleWorkRejected();
    }

    @Override
    public int getDistributedDoWorkAccepted() {
        return dwm.getDistributedStatistics().getDoWorkAccepted();
    }

    @Override
    public int getDistributedDoWorkRejected() {
        return dwm.getDistributedStatistics().getDoWorkRejected();
    }

    @Override
    public int getDistributedStartWorkAccepted() {
        return dwm.getDistributedStatistics().getStartWorkAccepted();
    }

    @Override
    public int getDistributedStartWorkRejected() {
        return dwm.getDistributedStatistics().getStartWorkRejected();
    }

    @Override
    public int getDistributedScheduleWorkAccepted() {
        return dwm.getDistributedStatistics().getScheduleWorkAccepted();
    }

    @Override
    public int getDistributedScheduleWorkRejected() {
        return dwm.getDistributedStatistics().getScheduleWorkRejected();
    }

    @Override
    public boolean isDoWorkDistributionEnabled() {
        return dwm.isDoWorkDistributionEnabled();
    }

    @Override
    public void doWork(Work work) throws WorkException {
        dwm.doWork(work);
    }

    @Override
    public void startWork(Work work) throws WorkException {
        dwm.startWork(work);
    }

    @Override
    public void scheduleWork(Work work) throws WorkException {
        dwm.scheduleWork(work);
    }
}

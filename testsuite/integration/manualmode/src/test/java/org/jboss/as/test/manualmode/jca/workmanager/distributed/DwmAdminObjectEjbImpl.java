/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.jboss.as.connector.services.workmanager.NamedDistributedWorkManager;
import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedAdminObject1;
import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedAdminObject1Impl;
import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedResourceAdapter1;

@Stateful(passivationCapable = false) //this is stateful so it maintains affinity, because standalone-ha.xml is used for the tests if a stateless bean is used invocations will go to other nodes
@Remote
public class DwmAdminObjectEjbImpl implements DwmAdminObjectEjb {

    @Resource(mappedName = "java:jboss/A1")
    private DistributedAdminObject1 dao;

    private NamedDistributedWorkManager dwm;

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

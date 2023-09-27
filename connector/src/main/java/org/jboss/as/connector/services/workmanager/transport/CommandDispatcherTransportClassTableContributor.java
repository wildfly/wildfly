/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import java.util.Arrays;
import java.util.List;

import jakarta.resource.spi.work.DistributableWork;
import jakarta.resource.spi.work.Work;

import org.jboss.jca.core.api.workmanager.DistributedWorkManagerStatisticsValues;
import org.jboss.jca.core.spi.workmanager.Address;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.jboss.ClassTableContributor;

/**
 * {@link ClassTableContributor} for a {@link CommandDispatcherTransport}.
 * @author Paul Ferraro
 */
@MetaInfServices(ClassTableContributor.class)
public class CommandDispatcherTransportClassTableContributor implements ClassTableContributor {

    @Override
    public List<Class<?>> getKnownClasses() {
        return Arrays.asList(Address.class, DistributedWorkManagerStatisticsValues.class,
                DistributableWork.class, Work.class, GetWorkManagersCommand.class,
                DeltaDoWorkAcceptedCommand.class, DeltaDoWorkRejectedCommand.class,
                DeltaScheduleWorkAcceptedCommand.class, DeltaScheduleWorkRejectedCommand.class,
                DeltaStartWorkAcceptedCommand.class, DeltaStartWorkRejectedCommand.class,
                DeltaWorkFailedCommand.class, DeltaWorkSuccessfulCommand.class,
                ClearDistributedStatisticsCommand.class, DistributedStatisticsCommand.class,
                PingCommand.class, LongRunningFreeCommand.class, ShortRunningFreeCommand.class,
                DoWorkCommand.class, StartWorkCommand.class, ScheduleWorkCommand.class,
                UpdateLongRunningFreeCommand.class, UpdateShortRunningFreeCommand.class,
                JoinCommand.class, LeaveCommand.class);
    }
}

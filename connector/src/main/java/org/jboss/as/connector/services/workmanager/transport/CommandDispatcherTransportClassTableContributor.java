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

package org.jboss.as.connector.services.workmanager.transport;

import java.util.Arrays;
import java.util.List;

import javax.resource.spi.work.DistributableWork;
import javax.resource.spi.work.Work;

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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkException;

public interface DwmAdminObjectEjb {
    int getDoWorkAccepted();

    int getDoWorkRejected();

    int getStartWorkAccepted();

    int getStartWorkRejected();

    int getScheduleWorkAccepted();

    int getScheduleWorkRejected();

    int getDistributedDoWorkAccepted();

    int getDistributedDoWorkRejected();

    int getDistributedStartWorkAccepted();

    int getDistributedStartWorkRejected();

    int getDistributedScheduleWorkAccepted();

    int getDistributedScheduleWorkRejected();

    boolean isDoWorkDistributionEnabled();

    void doWork(Work work) throws WorkException;

    void startWork(Work work) throws WorkException;

    void scheduleWork(Work work) throws WorkException;
}

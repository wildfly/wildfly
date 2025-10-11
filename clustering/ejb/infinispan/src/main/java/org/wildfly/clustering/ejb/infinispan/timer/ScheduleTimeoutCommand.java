/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.Map;

import org.wildfly.clustering.ejb.timer.TimeoutMetaData;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleCommand;

/**
 * Schedule command using persistent meta data.
 * @param <K> the scheduled entry key type
 * @author Paul Ferraro
 */
public class ScheduleTimeoutCommand<K> extends ScheduleCommand<K, TimeoutMetaData> {

    /**
     * Creates a command that schedules a timeout of the specified entry.
     * @param entry an entry to be scheduled
     */
    ScheduleTimeoutCommand(Map.Entry<K, TimeoutMetaData> entry) {
        super(entry);
    }

    @Override
    protected TimeoutMetaData getValue() {
        return new SimpleTimeoutMetaData(super.getValue());
    }
}

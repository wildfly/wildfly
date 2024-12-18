/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ejb.timer.TimeoutMetaData;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleWithPersistentMetaDataCommand;

/**
 * Schedule command using persistent meta data.
 * @author Paul Ferraro
 */
public class ScheduleWithPersistentTimeoutMetaDataCommand<I> extends ScheduleWithPersistentMetaDataCommand<I, TimeoutMetaData> {

    ScheduleWithPersistentTimeoutMetaDataCommand(I id, TimeoutMetaData metaData) {
        super(id, metaData);
    }

    @Override
    protected TimeoutMetaData getMetaData() {
        return new SimpleTimeoutMetaData(super.getMetaData());
    }
}

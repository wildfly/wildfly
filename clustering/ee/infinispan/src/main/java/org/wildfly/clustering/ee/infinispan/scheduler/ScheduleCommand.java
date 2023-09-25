/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Command that scheduled an element.
 * @author Paul Ferraro
 */
public interface ScheduleCommand<I, M> extends Command<Void, CacheEntryScheduler<I, M>> {

    /**
     * Returns the identifier of the element to be scheduled.
     * @return the identifier of the element to be scheduled.
     */
    I getId();

    /**
     * Returns the meta data of the element to be scheduled.
     * @return the meta data of the element to be scheduled.
     */
    M getMetaData();

    @Override
    default Void execute(CacheEntryScheduler<I, M> scheduler) throws Exception {
        I id = this.getId();
        M metaData = this.getMetaData();
        if (metaData != null) {
            scheduler.schedule(id, metaData);
        } else {
            scheduler.schedule(id);
        }
        return null;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.util.Collection;
import java.util.stream.Collectors;

import org.wildfly.clustering.dispatcher.Command;

/**
 * @author Paul Ferraro
 */
public class EntriesCommand<I, M> implements Command<Collection<I>, CacheEntryScheduler<I, M>> {
    private static final long serialVersionUID = -7918056022234250133L;

    @Override
    public Collection<I> execute(CacheEntryScheduler<I, M> scheduler) throws Exception {
        return scheduler.stream().collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}

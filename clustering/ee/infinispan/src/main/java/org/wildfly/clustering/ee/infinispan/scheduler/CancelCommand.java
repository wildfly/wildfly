/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Command that cancels a previously scheduled item.
 * @author Paul Ferraro
 */
public class CancelCommand<I, M> implements Command<Void, CacheEntryScheduler<I, M>> {
    private static final long serialVersionUID = 7990530622481705411L;

    private final I id;

    public CancelCommand(I id) {
        this.id = id;
    }

    I getId() {
        return this.id;
    }

    @Override
    public Void execute(CacheEntryScheduler<I, M> scheduler) {
        scheduler.cancel(this.id);
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", this.getClass().getSimpleName(), this.id);
    }
}

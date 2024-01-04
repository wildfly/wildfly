/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.server.infinispan.ClusteringServerLogger;

/**
 * A task that notifies a set of {@link GroupListener} instances.
 * @author Paul Ferraro
 */
public class GroupListenerNotificationTask implements Runnable, Consumer<GroupListener> {
    private final Iterable<Map.Entry<GroupListener, ExecutorService>> listeners;
    private final Membership previous;
    private final Membership current;
    private final boolean merged;

    public GroupListenerNotificationTask(Iterable<Map.Entry<GroupListener, ExecutorService>> listeners, Membership previous, Membership current, boolean merged) {
        this.listeners = listeners;
        this.previous = previous;
        this.current = current;
        this.merged = merged;
    }

    @Override
    public void run() {
        for (Map.Entry<GroupListener, ExecutorService> entry : this.listeners) {
            GroupListener listener = entry.getKey();
            ExecutorService executor = entry.getValue();
            try {
                executor.execute(() -> this.accept(listener));
            } catch (RejectedExecutionException e) {
                // Listener was unregistered
            }
        }
    }

    @Override
    public void accept(GroupListener listener) {
        try {
            listener.membershipChanged(this.previous, this.current, this.merged);
        } catch (Throwable e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }
}
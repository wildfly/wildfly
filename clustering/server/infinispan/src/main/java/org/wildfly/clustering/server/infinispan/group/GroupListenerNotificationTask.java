/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
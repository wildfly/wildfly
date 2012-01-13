/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.cache.impl.backing.clustering;

import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupMembershipNotifierRegistry;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public class GroupMembershipNotifierRegistryService implements GroupMembershipNotifierRegistry, Service<GroupMembershipNotifierRegistry> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("cluster").append("group-membership-notifier-registry");

    private final Map<String, GroupMembershipNotifier> groupMembershipNotifiers = Collections.synchronizedMap(new HashMap<String, GroupMembershipNotifier>());
    private final List<Listener> registryListeners = Collections.synchronizedList(new ArrayList<Listener>());

    @Override
    public void registerGroupMembershipNotifier(GroupMembershipNotifier groupMembershipNotifier) throws IllegalArgumentException {
        if (groupMembershipNotifier == null) {
            throw EjbMessages.MESSAGES.paramCannotBeNull("groupMembershipNotifier");
        }
        if (this.groupMembershipNotifiers.containsKey(groupMembershipNotifier.getGroupName())) {
            throw EjbMessages.MESSAGES.groupMembershipNotifierAlreadyRegistered(groupMembershipNotifier.getGroupName());
        }
        this.groupMembershipNotifiers.put(groupMembershipNotifier.getGroupName(), groupMembershipNotifier);

        // notify the listeners
        for (final Listener listener : this.registryListeners) {
            try {
                listener.newGroupMembershipNotifierRegistered(groupMembershipNotifier);
            } catch (Throwable t) {
                // log and ignore any errors
            }
        }
    }

    @Override
    public GroupMembershipNotifier getGroupMembershipNotifier(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw EjbMessages.MESSAGES.stringParamCannotBeNullOrEmpty("groupName");
        }
        return this.groupMembershipNotifiers.get(groupName);
    }

    @Override
    public void unregisterGroupMembershipNotifier(String groupName) throws IllegalArgumentException {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw EjbMessages.MESSAGES.stringParamCannotBeNullOrEmpty("groupName");
        }
        final GroupMembershipNotifier groupMembershipNotifier = this.groupMembershipNotifiers.remove(groupName);
        if (groupMembershipNotifier == null) {
            throw EjbMessages.MESSAGES.groupMembershipNotifierNotRegistered(groupName);
        }
        // notify the listeners
        for (final Listener listener : this.registryListeners) {
            try {
                listener.groupMembershipNotifierUnregistered(groupMembershipNotifier);
            } catch (Throwable t) {
                // log and ignore any errors
            }
        }
    }

    @Override
    public Iterable<GroupMembershipNotifier> getGroupMembershipNotifiers() {
        return Collections.unmodifiableCollection(this.groupMembershipNotifiers.values());
    }

    @Override
    public void addListener(final Listener listener) {
        this.registryListeners.add(listener);
    }

    @Override
    public boolean removeListener(final Listener listener) {
        return this.registryListeners.remove(listener);
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
        this.groupMembershipNotifiers.clear();
    }

    @Override
    public GroupMembershipNotifierRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}

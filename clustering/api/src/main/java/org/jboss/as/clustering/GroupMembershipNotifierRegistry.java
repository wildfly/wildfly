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

package org.jboss.as.clustering;

/**
 * A registry which keeps track of {@link GroupMembershipNotifier}s
 *
 * @author Jaikiran Pai
 */
public interface GroupMembershipNotifierRegistry {

    /**
     * Registers the passed <code>groupMembershipNotifier</code> into the registry. If a {@link GroupMembershipNotifier}
     * is already registered with the same {@link org.jboss.as.clustering.GroupMembershipNotifier#getGroupName() group name}
     * then this method throws an {@link IllegalArgumentException}.
     * <p/>
     * This method will invoke the {@link Listener#newGroupMembershipNotifierRegistered(GroupMembershipNotifier)}
     * callback method on all the listeners that have been added to this registry
     *
     * @param groupMembershipNotifier The group membership notifier
     * @throws IllegalArgumentException If the passed <code>groupMembershipNotifier</code> is null or if a {@link GroupMembershipNotifier}
     *                                  with the same group name is already registered
     */
    void registerGroupMembershipNotifier(final GroupMembershipNotifier groupMembershipNotifier) throws IllegalArgumentException;

    /**
     * Returns the {@link GroupMembershipNotifier} registered with the <code>groupName</code>, in this registry.
     * Returns null if no such {@link GroupMembershipNotifier} exists
     *
     * @param groupName The group name
     * @return
     */
    GroupMembershipNotifier getGroupMembershipNotifier(final String groupName);

    /**
     * Unregisters a {@link GroupMembershipNotifier} identified by the <code>groupName</code> from the registry.
     * <p/>
     * This method will invoke the {@link Listener#groupMembershipNotifierUnregistered(GroupMembershipNotifier)} callback
     * method on all listeners that have been added to this registry.
     *
     * @param groupName The group name
     * @throws IllegalArgumentException If the passed <code>groupName</code> is null or if the registry doesn't have a
     *                                  {@link GroupMembershipNotifier} registered by the <code>groupName</code>
     */
    void unregisterGroupMembershipNotifier(final String groupName) throws IllegalArgumentException;

    /**
     * Returns the {@link GroupMembershipNotifier}s that have been registered in this registry
     *
     * @return
     */
    Iterable<GroupMembershipNotifier> getGroupMembershipNotifiers();

    /**
     * Adds a {@link Listener} to this registry whose callback methods will be invoked when a new {@link GroupMembershipNotifier}
     * is added to the registry or a existing {@link GroupMembershipNotifier} is removed from the registry.
     *
     * @param listener The listener
     */
    void addListener(final Listener listener);

    /**
     * Removes the passed <code>listener</code> from the {@link Listener}s that have been added to this registry
     *
     * @param listener The listener to remove
     * @return Returns true if the <code>listener</code> being removed was indeed one among the listeners added to this registry.
     *         Else returns false
     */
    boolean removeListener(final Listener listener);

    /**
     * A listener for listening to {@link GroupMembershipNotifier} registration and unregistration events
     * in the {@link GroupMembershipNotifierRegistry}
     */
    public interface Listener {

        /**
         * A callback which will be invoked when a new {@link GroupMembershipNotifier} is registered in the
         * {@link GroupMembershipNotifierRegistry}
         *
         * @param groupMembershipNotifier The newly registered {@link GroupMembershipNotifier}
         */
        void newGroupMembershipNotifierRegistered(final GroupMembershipNotifier groupMembershipNotifier);

        /**
         * A callback which will be invoked when a {@link GroupMembershipNotifier} is unregistered from the
         * {@link GroupMembershipNotifierRegistry}
         *
         * @param groupMembershipNotifier The {@link GroupMembershipNotifier} which was unregistered
         */
        void groupMembershipNotifierUnregistered(final GroupMembershipNotifier groupMembershipNotifier);
    }
}

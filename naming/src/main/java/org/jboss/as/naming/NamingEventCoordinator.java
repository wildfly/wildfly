/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.naming.util.FastCopyHashMap;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jboss.threads.JBossThreadFactory;

import static java.security.AccessController.doPrivileged;

/**
 * Coordinator responsible for passing @(code NamingEvent} instances to registered @{code NamingListener} instances.  Two
 * maps are used to managed a mapping between a listener and its configuration as well as a mapping from target name to a list
 * of listener configurations.  These maps are updated atomically on listener add and remove.
 *
 * @author John E. Bailey
 */
public class NamingEventCoordinator {
    private volatile Map<TargetScope, List<ListenerHolder>> holdersByTarget = Collections.emptyMap();
    private volatile Map<NamingListener, ListenerHolder> holdersByListener = Collections.emptyMap();

    private final ThreadFactory threadFactory = doPrivileged(new PrivilegedAction<JBossThreadFactory>() {
        public JBossThreadFactory run() {
            return new JBossThreadFactory(new ThreadGroup("NamingEventCoordinator-threads"), Boolean.FALSE, null, "%G - %t", null, null);
        }
    });

    private final Executor executor = Executors.newSingleThreadExecutor(threadFactory);

    static final Integer[] DEFAULT_SCOPES = {EventContext.OBJECT_SCOPE, EventContext.ONELEVEL_SCOPE, EventContext.SUBTREE_SCOPE};

    /**
     * Add a listener to the coordinator with a given target name and event scope.  This information is used when an
     * event is fired to determine whether or not to fire this listener.
     *
     * @param target The target name to lister
     * @param scope The event scope
     * @param namingListener The listener
     */
    synchronized void addListener(final String target, final int scope, final NamingListener namingListener) {
        final TargetScope targetScope = new TargetScope(target, scope);
        // Do we have a holder for this listener
        ListenerHolder holder = holdersByListener.get(namingListener);
        if (holder == null) {
            holder = new ListenerHolder(namingListener, targetScope);
            final Map<NamingListener, ListenerHolder> byListenerCopy = new FastCopyHashMap<NamingListener, ListenerHolder>(holdersByListener);
            byListenerCopy.put(namingListener, holder);
            holdersByListener = byListenerCopy;
        } else {
            holder.addTarget(targetScope);
        }

        List<ListenerHolder> holdersForTarget = holdersByTarget.get(targetScope);
        if (holdersForTarget == null) {
            holdersForTarget = new CopyOnWriteArrayList<ListenerHolder>();
            final Map<TargetScope, List<ListenerHolder>> byTargetCopy = new FastCopyHashMap<TargetScope, List<ListenerHolder>>(holdersByTarget);
            byTargetCopy.put(targetScope, holdersForTarget);
            holdersByTarget = byTargetCopy;
        }
        holdersForTarget.add(holder);
    }

    /**
     * Remove a listener.  Will remove it from all target mappings.  Once this method returns, the listener will no longer
     * receive any events.
     *
     * @param namingListener The listener
     */
    synchronized void removeListener(final NamingListener namingListener) {
        // Do we have a holder for this listener
        final ListenerHolder holder = holdersByListener.get(namingListener);
        if (holder == null) {
            return;
        }

        final Map<NamingListener, ListenerHolder> byListenerCopy = new FastCopyHashMap<NamingListener, ListenerHolder>(holdersByListener);
        byListenerCopy.remove(namingListener);
        holdersByListener = byListenerCopy;

        final Map<TargetScope, List<ListenerHolder>> byTargetCopy = new FastCopyHashMap<TargetScope, List<ListenerHolder>>(holdersByTarget);
        for (TargetScope targetScope : holder.targets) {
            final List<ListenerHolder> holders = holdersByTarget.get(targetScope);
            holders.remove(holder);
            if (holders.isEmpty()) {
                byTargetCopy.remove(targetScope);
            }
        }
        holdersByTarget = byTargetCopy;
    }

    /**
     * Fire a naming event.  An event will be created with the provided information and sent to each listener that matches
     * the target and scope information.
     *
     * @param context The event context generating the event.
     * @param name The target name the event represents
     * @param existingBinding The existing binding at the provided name
     * @param newBinding The new binding at the provided name
     * @param type The event type
     * @param changeInfo The change info for the event
     * @param scopes The scopes this event should be fired against
     */
    void fireEvent(final EventContext context, final Name name, final Binding existingBinding, final Binding newBinding, int type, final String changeInfo, final Integer... scopes) {
        final String target = name.toString();
        final Set<Integer> scopeSet = new HashSet<Integer>(Arrays.asList(scopes));
        final NamingEvent event = new NamingEvent(context, type, newBinding, existingBinding, changeInfo);

        final Set<ListenerHolder> holdersToFire = new HashSet<ListenerHolder>();

        // Check for OBJECT_SCOPE based listeners
        if (scopeSet.contains(EventContext.OBJECT_SCOPE)) {
            final TargetScope targetScope = new TargetScope(target, EventContext.OBJECT_SCOPE);
            final List<ListenerHolder> holders = holdersByTarget.get(targetScope);
            if (holders != null) {
                for (ListenerHolder holder : holders) {
                    holdersToFire.add(holder);
                }
            }
        }

        // Check for ONELEVEL_SCOPE based listeners
        if (scopeSet.contains(EventContext.ONELEVEL_SCOPE) && !name.isEmpty()) {
            final TargetScope targetScope = new TargetScope(name.getPrefix(name.size() - 1).toString(), EventContext.ONELEVEL_SCOPE);
            final List<ListenerHolder> holders = holdersByTarget.get(targetScope);
            if (holders != null) {
                for (ListenerHolder holder : holders) {
                    holdersToFire.add(holder);
                }
            }
        }

        // Check for SUBTREE_SCOPE based listeners
        if (scopeSet.contains(EventContext.SUBTREE_SCOPE) && !name.isEmpty()) {
            for (int i = 1; i < name.size(); i++) {
                final Name parentName = name.getPrefix(i);
                final TargetScope targetScope = new TargetScope(parentName.toString(), EventContext.SUBTREE_SCOPE);
                final List<ListenerHolder> holders = holdersByTarget.get(targetScope);
                if (holders != null) {
                    for (ListenerHolder holder : holders) {
                        holdersToFire.add(holder);
                    }
                }
            }
        }

        executor.execute(new FireEventTask(holdersToFire, event));
    }

    private class FireEventTask implements Runnable {
        private final Set<ListenerHolder> listenerHolders;
        private final NamingEvent event;

        private FireEventTask(Set<ListenerHolder> listenerHolders, NamingEvent event) {
            this.listenerHolders = listenerHolders;
            this.event = event;
        }

        @Override
        public void run() {
            for (ListenerHolder holder : listenerHolders) {
                final NamingListener listener = holder.listener;
                switch (event.getType()) {
                    case NamingEvent.OBJECT_ADDED:
                        if (listener instanceof NamespaceChangeListener)
                            ((NamespaceChangeListener) listener).objectAdded(event);
                        break;
                    case NamingEvent.OBJECT_REMOVED:
                        if (listener instanceof NamespaceChangeListener)
                            ((NamespaceChangeListener) listener).objectRemoved(event);
                        break;
                    case NamingEvent.OBJECT_RENAMED:
                        if (listener instanceof NamespaceChangeListener)
                            ((NamespaceChangeListener) listener).objectRenamed(event);
                        break;
                    case NamingEvent.OBJECT_CHANGED:
                        if (listener instanceof ObjectChangeListener)
                            ((ObjectChangeListener) listener).objectChanged(event);
                        break;
                }
            }
        }
    }

    private class ListenerHolder {
        private volatile Set<TargetScope> targets = new HashSet<TargetScope>();
        private final NamingListener listener;

        private ListenerHolder(final NamingListener listener, final TargetScope initialTarget) {
            this.listener = listener;
            addTarget(initialTarget);
        }

        private synchronized void addTarget(final TargetScope targetScope) {
            targets.add(targetScope);
        }
    }

    private class TargetScope {
        private final String target;
        private final int scope;

        private TargetScope(String target, int scope) {
            this.target = target;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TargetScope that = (TargetScope) o;

            if (scope != that.scope) return false;
            if (target != null ? !target.equals(that.target) : that.target != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = target != null ? target.hashCode() : 0;
            result = 31 * result + scope;
            return result;
        }
    }
}

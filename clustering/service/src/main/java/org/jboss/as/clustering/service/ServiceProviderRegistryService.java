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

package org.jboss.as.clustering.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipListener;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.infinispan.atomic.AtomicMapCache;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener(sync = false)
public class ServiceProviderRegistryService implements ServiceProviderRegistry, GroupMembershipListener, Service<ServiceProviderRegistry> {

    private static final short SCOPE_ID = 224;

    public static ServiceName getServiceName(String name) {
        return CoreGroupCommunicationService.getServiceName(name).append("registry");
    }

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cacheRef = new InjectedValue<Cache>();
    private final InjectedValue<GroupMembershipNotifier> notifierRef = new InjectedValue<GroupMembershipNotifier>();
    private final Map<String, Listener> listeners = new ConcurrentHashMap<String, Listener>();

    private volatile GroupMembershipNotifier notifier;
    private volatile Cache<String, Map<ClusterNode, Void>> cache;

    public ServiceBuilder<ServiceProviderRegistry> build(ServiceTarget target, String container) {
        new CoreGroupCommunicationService(SCOPE_ID).build(target, container).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        return target.addService(getServiceName(container), this)
            .addDependency(CacheService.getServiceName(container, null), Cache.class, this.cacheRef)
            .addDependency(CoreGroupCommunicationService.getServiceName(container), GroupMembershipNotifier.class, this.notifierRef)
        ;
    }

    @Override
    public void register(final String service, Listener listener) {
        this.listeners.put(service, listener);
        final ClusterNode node = this.notifier.getClusterNode();
        Operation<Set<ClusterNode>> operation = new Operation<Set<ClusterNode>>() {
            @Override
            public Set<ClusterNode> invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                Map<ClusterNode, Void> map = cache.putIfAbsent(service, null);
                map.put(node, null);
                return map.keySet();
            }
        };
        Set<ClusterNode> nodes = this.invoke(operation);
        listener.serviceProvidersChanged(nodes, false);
    }

    @Override
    public void unregister(final String service) {
        final ClusterNode node = this.notifier.getClusterNode();
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                cache.get(service).remove(node);
                return null;
            }
        };
        this.invoke(operation);
        this.listeners.remove(service);
    }

    @Override
    public Set<ClusterNode> getServiceProviders(String service) {
        return Collections.unmodifiableSet(this.cache.get(service).keySet());
    }

    @Override
    public ServiceProviderRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.notifier = this.notifierRef.getValue();
        this.notifier.registerGroupMembershipListener(this);
        @SuppressWarnings("unchecked")
        Cache<String, Map<ClusterNode, Void>> cache = this.cacheRef.getValue();
        this.cache = new AtomicMapCache<String, ClusterNode, Void>(cache.getAdvancedCache().with(this.getClass().getClassLoader()));
        this.cache.addListener(this);
    }

    @Override
    public void stop(StopContext context) {
        this.cache.removeListener(this);
        this.notifier.unregisterGroupMembershipListener(this);
    }

    @Override
    public void membershipChanged(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers) {
        this.purgeDeadMembers(deadMembers, false);
    }

    @Override
    public void membershipChangedDuringMerge(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers, List<List<ClusterNode>> originatingGroups) {
        this.purgeDeadMembers(deadMembers, true);
    }

    private void purgeDeadMembers(final List<ClusterNode> deadNodes, final boolean merge) {
        Operation<List<Map.Entry<String, Set<ClusterNode>>>> operation = new Operation<List<Map.Entry<String, Set<ClusterNode>>>>() {
            @Override
            public List<Map.Entry<String, Set<ClusterNode>>> invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                // Collect services whose set of providing nodes has changed
                List<Map.Entry<String, Set<ClusterNode>>> entries = new ArrayList<Map.Entry<String, Set<ClusterNode>>>(cache.size());
                // Remove dead nodes for each service
                for (String key: cache.keySet()) {
                    Map<ClusterNode, Void> map = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).get(key);
                    if (map != null) {
                        Set<ClusterNode> nodes = map.keySet();
                        if (nodes.removeAll(deadNodes)) {
                            entries.add(new AbstractMap.SimpleImmutableEntry<String, Set<ClusterNode>>(key, nodes));
                        }
                    }
                }
                return entries;
            }
        };
        for (Map.Entry<String, Set<ClusterNode>> entry: this.invoke(operation)) {
            Listener listener = this.listeners.get(entry.getKey());
            if (listener != null) {
                listener.serviceProvidersChanged(entry.getValue(), merge);
            }
        }
    }

    @CacheEntryModified
    public void modified(CacheEntryModifiedEvent<String, Map<ClusterNode, Void>> event) {
        // Only respond to remote post-modify events
        if (event.isPre() || event.isOriginLocal()) return;
        final String service = event.getKey();
        Listener listener = this.listeners.get(service);
        if (listener != null) {
            listener.serviceProvidersChanged(event.getValue().keySet(), false);
        }
    }

    private <R> R invoke(Operation<R> operation) {
        return new BatchOperation<String, Map<ClusterNode, Void>, R>(operation).invoke(this.cache);
    }

    abstract class Operation<R> implements CacheInvoker.Operation<String, Map<ClusterNode, Void>, R> {
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.infinispan.atomic.AtomicMapCache;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener(sync = false)
public class ServiceProviderRegistryService implements ServiceProviderRegistry, ServiceProviderRegistryRpcHandler, GroupMembershipListener, Service<ServiceProviderRegistry> {

    public static ServiceName getServiceName(String name) {
        return CoreGroupCommunicationService.getServiceName(name).append("registry");
    }

    final ServiceName name;
    final ServiceProviderRegistryRpcHandler handler = new RpcDispatcher();
    @SuppressWarnings("rawtypes")
    private final Value<Cache> cacheRef;
    private final Value<GroupMembershipNotifier> notifierRef;
    private final Value<GroupRpcDispatcher> dispatcherRef;
    private final Map<String, Listener> listeners = new ConcurrentHashMap<String, Listener>();

    volatile GroupRpcDispatcher dispatcher;
    private volatile GroupMembershipNotifier notifier;
    private volatile Cache<String, Map<ClusterNode, Void>> cache;

    public ServiceProviderRegistryService(ServiceName name, @SuppressWarnings("rawtypes") Value<Cache> cacheRef, Value<GroupRpcDispatcher> dispatcherRef, Value<GroupMembershipNotifier> notifierRef) {
        this.name = name;
        this.cacheRef = cacheRef;
        this.notifierRef = notifierRef;
        this.dispatcherRef = dispatcherRef;
    }

    @Override
    public void register(final String service, Listener listener) {
        this.listeners.put(service, listener);
        final ClusterNode node = this.notifier.getClusterNode();
        Operation<Set<ClusterNode>> operation = new Operation<Set<ClusterNode>>() {
            @Override
            public Set<ClusterNode> invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                Map<ClusterNode, Void> map = cache.putIfAbsent(service, null);
                if (!map.containsKey(node)) {
                    map.put(node, null);
                }
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
    public List<String> getServices(final ClusterNode node) {
        Operation<List<String>> operation = new Operation<List<String>>() {
            @Override
            public List<String> invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                Set<String> services = cache.keySet();
                List<String> result = new ArrayList<String>(services.size());
                for (String service: services) {
                    if (cache.get(service).keySet().contains(node)) {
                        result.add(service);
                    }
                }
                return result;
            }
        };
        return this.invoke(operation);
    }

    @Override
    public ServiceProviderRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.dispatcher = this.dispatcherRef.getValue();
        this.dispatcher.registerRPCHandler(this.name.getCanonicalName(), this);
        this.notifier = this.notifierRef.getValue();
        this.notifier.registerGroupMembershipListener(this);
        @SuppressWarnings("unchecked")
        Cache<String, Map<ClusterNode, Void>> cache = this.cacheRef.getValue();
        this.cache = new AtomicMapCache<String, ClusterNode, Void>(cache.getAdvancedCache());
        this.cache.addListener(this);
    }

    @Override
    public void stop(StopContext context) {
        this.cache.removeListener(this);
        this.notifier.unregisterGroupMembershipListener(this);
        this.dispatcher.unregisterRPCHandler(this.name.getCanonicalName(), this);
    }

    @Override
    public void membershipChanged(final List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers) {
        Operation<Map<String, Set<ClusterNode>>> operation = new Operation<Map<String, Set<ClusterNode>>>() {
            @Override
            public Map<String, Set<ClusterNode>> invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                // Collect service provider updates
                Map<String, Set<ClusterNode>> updates = new HashMap<String, Set<ClusterNode>>();
                ServiceProviderRegistryService.this.purgeDeadMembers(deadMembers, updates);
                return updates;
            }
        };
        this.notifyListeners(this.invoke(operation), false);
    }

    @Override
    public void membershipChangedDuringMerge(final List<ClusterNode> deadMembers, final List<ClusterNode> newMembers, List<ClusterNode> allMembers, final List<List<ClusterNode>> originatingGroups) {
        Operation<Map<String, Set<ClusterNode>>> operation = new Operation<Map<String, Set<ClusterNode>>>() {
            @Override
            public Map<String, Set<ClusterNode>> invoke(Cache<String, Map<ClusterNode, Void>> cache) {
                // Collect service provider updates
                Map<String, Set<ClusterNode>> updates = new HashMap<String, Set<ClusterNode>>();
                if (newMembers.isEmpty()) {
                    for (String service: cache.keySet()) {
                        updates.put(service, cache.get(service).keySet());
                    }
                } else {
                    for (ClusterNode node: newMembers) {
                        // Re-assert services for new members following merge since these may have been lost following split
                        List<String> services = ServiceProviderRegistryService.this.handler.getServices(node);
                        for (String service: services) {
                            Map<ClusterNode, Void> nodes = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).putIfAbsent(service, null);
                            nodes.put(node, null);
                            updates.put(service, Collections.unmodifiableSet(nodes.keySet()));
                        }
                    }
                }
                ServiceProviderRegistryService.this.purgeDeadMembers(deadMembers, updates);
                return updates;
            }
        };
        this.notifyListeners(this.invoke(operation), true);
    }

    void purgeDeadMembers(List<ClusterNode> deadNodes, Map<String, Set<ClusterNode>> updates) {
        // Remove dead nodes for each service
        for (String key: cache.keySet()) {
            Map<ClusterNode, Void> map = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).get(key);
            if (map != null) {
                Set<ClusterNode> nodes = map.keySet();
                if (nodes.removeAll(deadNodes)) {
                    updates.put(key, Collections.unmodifiableSet(nodes));
                }
            }
        }
    }

    private void notifyListeners(Map<String, Set<ClusterNode>> updates, boolean merge) {
        for (Map.Entry<String, Set<ClusterNode>> entry: updates.entrySet()) {
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
        this.notifyListeners(Collections.singletonMap(event.getKey(), event.getValue().keySet()), false);
    }

    private <R> R invoke(Operation<R> operation) {
        return new BatchOperation<String, Map<ClusterNode, Void>, R>(operation).invoke(this.cache);
    }

    static interface Operation<R> extends CacheInvoker.Operation<String, Map<ClusterNode, Void>, R> {
    }

    class RpcDispatcher implements ServiceProviderRegistryRpcHandler {
        @Override
        public List<String> getServices(ClusterNode node) {
            try {
                return ServiceProviderRegistryService.this.dispatcher.callMethodOnNode(ServiceProviderRegistryService.this.name.getCanonicalName(), "getServices", new Object[] { node }, new Class[] { ClusterNode.class }, node);
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }
}

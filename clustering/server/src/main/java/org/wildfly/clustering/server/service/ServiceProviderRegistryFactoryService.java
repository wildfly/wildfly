/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.Node;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.MembershipListener;
import org.wildfly.clustering.service.ServiceProviderRegistration;
import org.wildfly.clustering.service.ServiceProviderRegistry;
import org.wildfly.clustering.service.ServiceProviderRegistration.Listener;

/**
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener
public class ServiceProviderRegistryFactoryService implements ServiceProviderRegistry, ServiceProvider, MembershipListener, Service<ServiceProviderRegistry> {

    private final ServiceName name;
    @SuppressWarnings("rawtypes")
    private final Value<Cache> cacheRef;
    final Value<CommandDispatcherFactory> dispatcherFactory;
    final ConcurrentMap<ServiceName, Listener> listeners = new ConcurrentHashMap<>();
    final CacheInvoker invoker;

    private volatile CommandDispatcher<ServiceProvider> dispatcher;
    volatile Cache<ServiceName, Set<Node>> cache;

    public ServiceProviderRegistryFactoryService(ServiceName name, @SuppressWarnings("rawtypes") Value<Cache> cacheRef, Value<CommandDispatcherFactory> dispatcherFactory, CacheInvoker invoker) {
        this.name = name;
        this.cacheRef = cacheRef;
        this.dispatcherFactory = dispatcherFactory;
        this.invoker = invoker;
    }

    @Override
    public ServiceProviderRegistration createRegistration(final ServiceName service, Listener listener) {
        if (this.listeners.putIfAbsent(service, listener) != null) {
            throw new IllegalArgumentException(service.getCanonicalName());
        }
        final Node node = this.dispatcherFactory.getValue().getLocalNode();
        Operation<Set<Node>> operation = new Operation<Set<Node>>() {
            @Override
            public Set<Node> invoke(Cache<ServiceName, Set<Node>> cache) {
                Set<Node> nodes = new HashSet<>(Collections.singleton(node));
                Set<Node> existing = cache.putIfAbsent(service, nodes);
                if (existing != null) {
                    if (existing.add(node)) {
                        cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).replace(service, existing);
                    }
                }
                return (existing != null) ? existing : nodes;
            }
        };
        this.invoker.invoke(this.cache, operation);
        return new ServiceProviderRegistrationImpl(service);
    }

    @Override
    public Set<Node> getServiceProviders(final ServiceName service) {
        Operation<Set<Node>> operation = new Operation<Set<Node>>() {
            @Override
            public Set<Node> invoke(Cache<ServiceName, Set<Node>> cache) {
                return cache.get(service);
            }
        };
        Set<Node> nodes = this.invoker.invoke(this.cache, operation);
        return (nodes != null) ? Collections.unmodifiableSet(nodes) : Collections.<Node>emptySet();
    }

    @Override
    public Set<ServiceName> getServices() {
        return this.listeners.keySet();
    }

    @Override
    public ServiceProviderRegistry getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) {
        this.dispatcher = this.dispatcherFactory.getValue().<ServiceProvider>createCommandDispatcher(this.name, this, this);
        this.cache = this.cacheRef.getValue();
        this.cache.addListener(this);
    }

    @Override
    public void stop(StopContext context) {
        this.cache.removeListener(this);
        this.dispatcher.close();
    }

    @Override
    public void membershipChanged(final List<Node> deadNodes, final List<Node> newNodes, List<Node> allNodes, final List<List<Node>> groups) {
        if (this.dispatcherFactory.getValue().isCoordinator()) {
            Operation<Void> operation = new Operation<Void>() {
                @Override
                public Void invoke(Cache<ServiceName, Set<Node>> cache) {
                    if (!deadNodes.isEmpty()) {
                        for (ServiceName service: cache.keySet()) {
                            Set<Node> nodes = cache.get(service);
                            if (nodes != null) {
                                if (nodes.removeAll(deadNodes)) {
                                    cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).replace(service, nodes);
                                }
                            }
                        }
                    }
                    if (groups != null) {
                        for (Node node: newNodes) {
                            // Re-assert services for new members following merge since these may have been lost following split
                            List<ServiceName> services = ServiceProviderRegistryFactoryService.this.getServices(node);
                            for (ServiceName service: services) {
                                Set<Node> nodes = new HashSet<>(Collections.singleton(node));
                                Set<Node> existing = cache.putIfAbsent(service, nodes);
                                if (existing != null) {
                                    if (existing.add(node)) {
                                        cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).replace(service, existing);
                                    }
                                }
                            }
                        }
                    }
                    return null;
                }
            };
            this.invoker.invoke(this.cache, operation);
        }
    }

    @CacheEntryModified
    public void modified(CacheEntryModifiedEvent<ServiceName, Set<Node>> event) {
        // Only respond to remote post-modify events
        if (event.isPre()) return;
        Listener listener = this.listeners.get(event.getKey());
        if (listener != null) {
            listener.serviceProvidersChanged(event.getValue());
        }
    }

    List<ServiceName> getServices(Node node) {
        try {
            return this.dispatcher.executeOnNode(new ServiceProviderCommand(), node).get();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private class ServiceProviderRegistrationImpl implements ServiceProviderRegistration {
        final ServiceName serviceName;

        ServiceProviderRegistrationImpl(ServiceName serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public Set<Node> getServiceProviders() {
            return ServiceProviderRegistryFactoryService.this.getServiceProviders(this.serviceName);
        }

        @Override
        public void close() {
            if (ServiceProviderRegistryFactoryService.this.listeners.remove(this.serviceName) != null) {
                final Node node = ServiceProviderRegistryFactoryService.this.dispatcherFactory.getValue().getLocalNode();
                Operation<Void> operation = new Operation<Void>() {
                    @Override
                    public Void invoke(Cache<ServiceName, Set<Node>> cache) {
                        Set<Node> nodes = cache.get(ServiceProviderRegistrationImpl.this.serviceName);
                        if ((nodes != null) && nodes.remove(node)) {
                            if (nodes.isEmpty()) {
                                cache.remove(ServiceProviderRegistrationImpl.this.serviceName);
                            } else {
                                cache.replace(ServiceProviderRegistrationImpl.this.serviceName, nodes);
                            }
                        }
                        return null;
                    }
                };
                ServiceProviderRegistryFactoryService.this.invoker.invoke(ServiceProviderRegistryFactoryService.this.cache, operation, Flag.IGNORE_RETURN_VALUES);
            }
        }
    }

    interface Operation<R> extends CacheInvoker.Operation<ServiceName, Set<Node>, R> {
    }
}

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
package org.wildfly.clustering.server.provider;

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
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;

/**
 * Service provider registration factory implementation.
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener
public class ServiceProviderRegistrationFactoryService implements ServiceProviderRegistrationFactory, ServiceRegistry, Group.Listener, Service<ServiceProviderRegistrationFactory> {

    private final ServiceProviderRegistrationFactoryConfiguration config;

    final ConcurrentMap<Object, Listener> listeners = new ConcurrentHashMap<>();

    volatile Group group = null;
    volatile Cache<Object, Set<Node>> cache = null;
    volatile CacheInvoker invoker = null;

    private volatile CommandDispatcher<ServiceRegistry> dispatcher = null;

    public ServiceProviderRegistrationFactoryService(ServiceProviderRegistrationFactoryConfiguration config) {
        this.config = config;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public ServiceProviderRegistration createRegistration(final Object service, Listener listener) {
        if (this.listeners.putIfAbsent(service, listener) != null) {
            throw new IllegalArgumentException(service.toString());
        }
        final Node node = this.group.getLocalNode();
        Operation<Set<Node>> operation = new Operation<Set<Node>>() {
            @Override
            public Set<Node> invoke(Cache<Object, Set<Node>> cache) {
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
        return new ServiceProviderRegistration() {
            @Override
            public Object getService() {
                return service;
            }

            @Override
            public Set<Node> getProviders() {
                return ServiceProviderRegistrationFactoryService.this.getProviders(service);
            }

            @Override
            public void close() {
                if (ServiceProviderRegistrationFactoryService.this.listeners.remove(service) != null) {
                    final Node node = ServiceProviderRegistrationFactoryService.this.group.getLocalNode();
                    Operation<Void> operation = new Operation<Void>() {
                        @Override
                        public Void invoke(Cache<Object, Set<Node>> cache) {
                            Set<Node> nodes = cache.get(service);
                            if ((nodes != null) && nodes.remove(node)) {
                                if (nodes.isEmpty()) {
                                    cache.remove(service);
                                } else {
                                    cache.replace(service, nodes);
                                }
                            }
                            return null;
                        }
                    };
                    ServiceProviderRegistrationFactoryService.this.invoker.invoke(ServiceProviderRegistrationFactoryService.this.cache, operation, Flag.IGNORE_RETURN_VALUES);
                }
            }
        };
    }

    @Override
    public Set<Node> getProviders(final Object service) {
        Operation<Set<Node>> operation = new Operation<Set<Node>>() {
            @Override
            public Set<Node> invoke(Cache<Object, Set<Node>> cache) {
                return cache.get(service);
            }
        };
        Set<Node> nodes = this.invoker.invoke(this.cache, operation);
        return (nodes != null) ? Collections.unmodifiableSet(nodes) : Collections.<Node>emptySet();
    }

    @Override
    public Set<Object> getServices() {
        return this.listeners.keySet();
    }

    @Override
    public ServiceProviderRegistrationFactory getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) {
        this.invoker = this.config.getCacheInvoker();
        this.group = this.config.getGroup();
        this.cache = this.config.getCache();
        this.dispatcher = this.config.getCommandDispatcherFactory().<ServiceRegistry>createCommandDispatcher(this.config.getId(), this);
        this.cache.addListener(this);
        this.group.addListener(this);
    }

    @Override
    public void stop(StopContext context) {
        this.group.removeListener(this);
        this.cache.removeListener(this);
        this.dispatcher.close();
    }

    @Override
    public void membershipChanged(List<Node> previousMembers, List<Node> members, final boolean merged) {
        if (this.group.isCoordinator()) {
            final Set<Node> deadNodes = new HashSet<>(previousMembers);
            deadNodes.removeAll(members);
            final Set<Node> newNodes = new HashSet<>(members);
            newNodes.removeAll(previousMembers);
            Operation<Void> operation = new Operation<Void>() {
                @Override
                public Void invoke(Cache<Object, Set<Node>> cache) {
                    if (!deadNodes.isEmpty()) {
                        for (Object service: cache.keySet()) {
                            Set<Node> nodes = cache.get(service);
                            if (nodes != null) {
                                if (nodes.removeAll(deadNodes)) {
                                    cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).replace(service, nodes);
                                }
                            }
                        }
                    }
                    if (merged) {
                        for (Node node: newNodes) {
                            // Re-assert services for new members following merge since these may have been lost following split
                            List<Object> services = ServiceProviderRegistrationFactoryService.this.getServices(node);
                            for (Object service: services) {
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
        if (event.isPre()) return;
        Listener listener = this.listeners.get(event.getKey());
        if (listener != null) {
            listener.providersChanged(event.getValue());
        }
    }

    List<Object> getServices(Node node) {
        try {
            return this.dispatcher.executeOnNode(new ServiceRegistryCommand(), node).get();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    interface Operation<R> extends CacheInvoker.Operation<Object, Set<Node>, R> {
    }
}

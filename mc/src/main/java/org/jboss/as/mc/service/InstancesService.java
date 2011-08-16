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

package org.jboss.as.mc.service;

import org.jboss.as.mc.BeanState;
import org.jboss.as.mc.descriptor.BeanMetaDataConfig;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Available instances per type
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class InstancesService implements Service<Set<Object>> {
    private static final Logger log = Logger.getLogger(InstancesService.class);

    private final Set<Object> instances = new HashSet<Object>(); // we do own locking
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final ReadWriteLock callbackLock = new ReentrantReadWriteLock();
    private static Map<Class<?>, Map<BeanState, List<Callback>>> incallbacks = new HashMap<Class<?>, Map<BeanState, List<Callback>>>();
    private static Map<Class<?>, Map<BeanState, List<Callback>>> uncallbacks = new HashMap<Class<?>, Map<BeanState, List<Callback>>>();

    private InstancesService() {
    }

    /**
     * Add bean instance.
     *
     * @param registry the service registry
     * @param target the service target
     * @param state the bean state
     * @param bean the bean
     * @throws StartException for any error
     */
    static void addInstance(ServiceRegistry registry, ServiceTarget target, BeanState state, Object bean) throws StartException {
        addInstance(registry, target, state, bean.getClass(), bean);
    }

    /**
     * Remove bean instance.
     *
     * @param registry the service registry
     * @param state the bean state
     * @param bean the bean
     */
    static void removeInstance(ServiceRegistry registry, BeanState state, Object bean) {
        removeInstance(registry, state, bean.getClass(), bean);
    }

    /**
     * Add incallback.
     *
     * @param callback the callback
     */
    static void addIncallback(Callback callback) {
        try {
            callback.dispatch(); // check all previous
        } catch (Throwable t) {
            log.warn("Error invoking incallback: " + callback, t);
        }
        addCallback(incallbacks, callback);
    }

    /**
     * Add uncallback.
     *
     * @param callback the callback
     */
    static void addUncallback(Callback callback) {
        addCallback(uncallbacks, callback);
    }

    /**
     * Remove incallback.
     *
     * @param callback the callback
     */
    static void removeIncallback(Callback callback) {
        removeCallback(incallbacks, callback);
    }

    /**
     * Remove uncallback.
     *
     * @param callback the callback
     */
    static void removeUncallback(Callback callback) {
        removeCallback(uncallbacks, callback);
        try {
            callback.dispatch(); // remove all existing
        } catch (Throwable t) {
            log.warn("Error invoking uncallback: " + callback, t);
        }
    }

    private static void addCallback(Map<Class<?>, Map<BeanState, List<Callback>>> map, Callback callback) {
        callbackLock.writeLock().lock();
        try {
            Map<BeanState, List<Callback>> states = map.get(callback.getType());
            if (states == null) {
                states = new HashMap<BeanState, List<Callback>>();
                map.put(callback.getType(), states);
            }
            List<Callback> callbacks = states.get(callback.getState());
            if (callbacks == null) {
                callbacks = new ArrayList<Callback>();
                states.put(callback.getState(), callbacks);
            }
            callbacks.add(callback);
        } finally {
            callbackLock.writeLock().unlock();
        }
    }

    private static void removeCallback(Map<Class<?>, Map<BeanState, List<Callback>>> map, Callback callback) {
        callbackLock.writeLock().lock();
        try {
            Map<BeanState, List<Callback>> states = map.get(callback.getType());
            if (states != null) {
                List<Callback> callbacks = states.get(callback.getState());
                if (callbacks != null) {
                    callbacks.remove(callback);
                    if (callbacks.isEmpty())
                        states.remove(callback.getState());
                }
                if (states.isEmpty())
                    map.remove(callback.getType());
            }
        } finally {
            callbackLock.writeLock().unlock();
        }
    }

    private static void invokeCallbacks(Map<Class<?>, Map<BeanState, List<Callback>>> map, BeanState state, Class<?> clazz, Object bean) {
        callbackLock.readLock().lock();
        try {
            Map<BeanState, List<Callback>> states = map.get(clazz);
            if (states != null) {
                List<Callback> callbacks = states.get(state);
                if (callbacks != null)
                    for (Callback c : callbacks)
                        try {
                            c.dispatch(bean);
                        } catch (Throwable t) {
                            log.warn("Error invoking callback: " + c, t);
                        }
            }
        } finally {
            callbackLock.readLock().unlock();
        }
    }

    private static void addInstance(ServiceRegistry registry, ServiceTarget target, BeanState state, Class<?> clazz, Object bean) throws StartException {
        if (clazz == null)
            return;

        ServiceName name = BeanMetaDataConfig.toInstancesName(clazz, state);
        ServiceBuilder<Set<Object>> builder = target.addService(name, new InstancesService());
        InstancesService service = putIfAbsent(registry, name, builder);
        service.lock.writeLock().lock();
        try {
            service.instances.add(bean);
            invokeCallbacks(incallbacks, state, clazz, bean);
        } finally {
            service.lock.writeLock().unlock();
        }

        addInstance(registry, target, state, clazz.getSuperclass(), bean);
        Class<?>[] ifaces = clazz.getInterfaces();
        for (Class<?> iface : ifaces)
            addInstance(registry, target, state, iface, bean);
    }

    private static InstancesService putIfAbsent(ServiceRegistry registry, ServiceName name, ServiceBuilder builder) throws StartException {
        for (;;) {
            try {
                ServiceController sc = registry.getService(name);
                if (sc == null) {
                    sc = builder.install();
                }
                return (InstancesService) sc.getService();
            } catch (DuplicateServiceException ignored) {
            } catch (Exception e) {
                throw new StartException(e);
            }
        }
    }

    private static void removeInstance(ServiceRegistry registry, BeanState state, Class<?> clazz, Object bean) {
        if (clazz == null)
            return;

        ServiceController controller = registry.getService(BeanMetaDataConfig.toInstancesName(clazz, state));
        if (controller != null) {
            InstancesService service = (InstancesService) controller.getService();
            service.lock.writeLock().lock();
            try {
                service.instances.remove(bean);
                invokeCallbacks(uncallbacks, state, clazz, bean);
                if (service.instances.isEmpty())
                    controller.setMode(ServiceController.Mode.REMOVE);
            } finally {
                service.lock.writeLock().unlock();
            }
        }

        removeInstance(registry, state, clazz.getSuperclass(), bean);
        Class<?>[] ifaces = clazz.getInterfaces();
        for (Class<?> iface : ifaces)
            removeInstance(registry, state, iface, bean);
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public Set<Object> getValue() throws IllegalStateException, IllegalArgumentException {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableSet(instances);
        } finally {
            lock.readLock().unlock();
        }
    }
}

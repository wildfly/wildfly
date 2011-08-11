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

import org.jboss.as.mc.descriptor.BeanMetaDataConfig;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Available instances per type
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class InstancesService implements Service<Set<Object>> {
    private final Set<Object> instances = new HashSet<Object>(); // we do own locking
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private InstancesService() {
    }

    static void addInstance(ServiceTarget target, Object bean) {
        addInstance(target, bean.getClass(), bean);
    }

    static void removeInstance(ServiceRegistry registry, Object bean) {
        removeInstance(registry, bean.getClass(), bean);
    }

    private static void addInstance(ServiceTarget target, Class<?> clazz, Object bean) {
        if (clazz == null)
            return;

        ServiceBuilder<Set<Object>> builder = target.addService(BeanMetaDataConfig.toInstancesName(clazz), new InstancesService());
        ServiceController<Set<Object>> controller = builder.install(); // TODO -- putIfAbsent, DML's REMOVE->ACTIVE+listener
        InstancesService service = (InstancesService) controller.getService();
        service.lock.writeLock().lock();
        try {
            service.instances.add(bean);
        } finally {
            service.lock.writeLock().unlock();
        }

        addInstance(target, clazz.getSuperclass(), bean);
        Class<?>[] ifaces = clazz.getInterfaces();
        for (Class<?> iface : ifaces)
            addInstance(target, iface, bean);
    }

    private static void removeInstance(ServiceRegistry registry, Class<?> clazz, Object bean) {
        if (clazz == null)
            return;

        ServiceController controller = registry.getService(BeanMetaDataConfig.toInstancesName(clazz));
        if (controller != null) {
            InstancesService service = (InstancesService) controller.getService();
            service.lock.writeLock().lock();
            try {
                service.instances.remove(bean);
                if (service.instances.isEmpty())
                    controller.setMode(ServiceController.Mode.REMOVE);
            } finally {
                service.lock.writeLock().unlock();
            }
        }

        removeInstance(registry, clazz.getSuperclass(), bean);
        Class<?>[] ifaces = clazz.getInterfaces();
        for (Class<?> iface : ifaces)
            removeInstance(registry, iface, bean);
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

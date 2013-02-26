/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Registry that can be used to directly look up a component, based on the component class.
 * <p/>
 * This is obviously not ideal, as it is possible to have multiple components for a single class,
 * however it is necessary to work around problematic SPI's that expect to be able to inject / instantiate
 * based only on the class type.
 * <p/>
 * This registry only contains simple component types that do not have a view
 *
 * @author Stuart Douglas
 */
public class ComponentRegistry {

    public static ServiceName SERVICE_NAME = ServiceName.of("ee", "ComponentRegistry");

    private final Map<Class<?>, ComponentManagedReferenceFactory> componentsByClass = new ConcurrentHashMap<Class<?>, ComponentManagedReferenceFactory>();
    private final ServiceRegistry serviceRegistry;

    public ComponentRegistry(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void addComponent(final ComponentConfiguration componentConfiguration) {
        componentsByClass.put(componentConfiguration.getComponentClass(), new ComponentManagedReferenceFactory(componentConfiguration.getComponentDescription().getStartServiceName()));
    }

    public ManagedReference createInstance(final Class<?> componentClass) {
        final ManagedReferenceFactory factory = componentsByClass.get(componentClass);
        if (factory == null) {
            return null;
        }
        return factory.getReference();
    }

    public ManagedReference createInstance(final Object instance) {
        final ComponentManagedReferenceFactory factory = componentsByClass.get(instance.getClass());
        if (factory == null) {
            return null;
        }
        return factory.getReference(instance);
    }

    public Map<Class<?>, ComponentManagedReferenceFactory> getComponentsByClass() {
        return Collections.unmodifiableMap(componentsByClass);
    }

    private static class ComponentManagedReference implements ManagedReference {

        private final ComponentInstance instance;
        private boolean destroyed;

        public ComponentManagedReference(final ComponentInstance component) {
            instance = component;
        }

        @Override
        public synchronized void release() {
            if (!destroyed) {
                instance.destroy();
                destroyed = true;
            }
        }

        @Override
        public Object getInstance() {
            return instance.getInstance();
        }
    }

    public class ComponentManagedReferenceFactory implements ManagedReferenceFactory {

        private final ServiceName serviceName;
        private volatile ServiceController<Component> component;

        private ComponentManagedReferenceFactory(final ServiceName serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public ManagedReference getReference() {
            if (component == null) {
                synchronized (this) {
                    if (component == null) {
                        component = (ServiceController<Component>) serviceRegistry.getService(serviceName);
                    }
                }
            }
            if (component == null) {
                return null;
            }
            return new ComponentManagedReference(component.getValue().createInstance());
        }


        public ManagedReference getReference(final Object instance) {
            if (component == null) {
                synchronized (this) {
                    if (component == null) {
                        component = (ServiceController<Component>) serviceRegistry.getService(serviceName);
                    }
                }
            }
            if (component == null) {
                return null;
            }
            return new ComponentManagedReference(component.getValue().createInstance(instance));
        }

        public ServiceName getServiceName() {
            return serviceName;
        }
    }
}

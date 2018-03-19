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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.InjectedValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that can be used to create a fully injected class instance. If there is an appropriate component regiestered
 * an instance of the component will be created. Otherwise the default class introspector will be used to create an instance.
 * <p/>
 * This can be problematic in theory, as it is possible to have multiple components for a single class, however it does
 * not seem to be an issue in practice.
 * <p/>
 * This registry only contains simple component types that have at most 1 view
 *
 * @author Stuart Douglas
 */
public class ComponentRegistry {

    private static ServiceName SERVICE_NAME = ServiceName.of("ee", "ComponentRegistry");

    private final Map<Class<?>, ComponentManagedReferenceFactory> componentsByClass = new ConcurrentHashMap<Class<?>, ComponentManagedReferenceFactory>();
    private final ServiceRegistry serviceRegistry;
    private final InjectedValue<EEClassIntrospector> classIntrospectorInjectedValue = new InjectedValue<>();

    public static ServiceName serviceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(SERVICE_NAME);
    }

    public ComponentRegistry(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void addComponent(final ComponentConfiguration componentConfiguration) {
        if(componentConfiguration.getViews().size() < 2) {
            if(componentConfiguration.getViews().size() == 0) {
                componentsByClass.put(componentConfiguration.getComponentClass(), new ComponentManagedReferenceFactory(componentConfiguration.getComponentDescription().getStartServiceName(), null));
            } else {
                componentsByClass.put(componentConfiguration.getComponentClass(), new ComponentManagedReferenceFactory(componentConfiguration.getComponentDescription().getStartServiceName(), componentConfiguration.getViews().get(0).getViewServiceName()));
            }
        }
    }

    public ManagedReferenceFactory createInstanceFactory(final Class<?> componentClass) {
        final ManagedReferenceFactory factory = componentsByClass.get(componentClass);
        if (factory == null) {
            return classIntrospectorInjectedValue.getValue().createFactory(componentClass);
        }
        return factory;
    }

    public ManagedReference createInstance(final Object instance) {

        final ComponentManagedReferenceFactory factory = componentsByClass.get(instance.getClass());
        if (factory == null) {
            return classIntrospectorInjectedValue.getValue().createInstance(instance);
        }
        return factory.getReference(instance);
    }

    public InjectedValue<EEClassIntrospector> getClassIntrospectorInjectedValue() {
        return classIntrospectorInjectedValue;
    }

    private static class ComponentManagedReference implements ManagedReference {

        private final ComponentInstance instance;
        private boolean destroyed;

        ComponentManagedReference(final ComponentInstance component) {
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
        private final ServiceName viewServiceName;
        private volatile ServiceController<Component> component;
        private volatile ServiceController<ViewService.View> view;

        private ComponentManagedReferenceFactory(final ServiceName serviceName, ServiceName viewServiceName) {
            this.serviceName = serviceName;
            this.viewServiceName = viewServiceName;
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
            if (view == null && viewServiceName != null) {
                synchronized (this) {
                    if (view == null) {
                        view = (ServiceController<ViewService.View>) serviceRegistry.getService(viewServiceName);
                    }
                }
            }
            if (component == null) {
                return null;
            }
            if(view == null) {
                return new ComponentManagedReference(component.getValue().createInstance());
            } else {
                try {
                    return view.getValue().createInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
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

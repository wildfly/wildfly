/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.util.reflection.HierarchyDiscovery;
import org.jboss.weld.util.reflection.Reflections;

public abstract class AbstractResourceInjectionServices {

    protected final ServiceRegistry serviceRegistry;
    protected final EEModuleDescription moduleDescription;
    private final Module module;

    protected AbstractResourceInjectionServices(ServiceRegistry serviceRegistry, EEModuleDescription moduleDescription, Module module) {
        this.serviceRegistry = serviceRegistry;
        this.moduleDescription = moduleDescription;
        this.module = module;
    }

    protected abstract ContextNames.BindInfo getBindInfo(final String result);

    protected ManagedReferenceFactory getManagedReferenceFactory(ContextNames.BindInfo ejbBindInfo) {
        try {
            ServiceController<?> controller = serviceRegistry.getRequiredService(ejbBindInfo.getBinderServiceName());
            return (ManagedReferenceFactory) controller.getValue();
        } catch (Exception e) {
            return null;
        }
    }

    protected ResourceReferenceFactory<Object> handleServiceLookup(final String result, InjectionPoint injectionPoint) {
        final ContextNames.BindInfo ejbBindInfo = getBindInfo(result);

        /*
         * Try to obtain ManagedReferenceFactory and validate the resource type
         */
        final ManagedReferenceFactory factory = getManagedReferenceFactory(ejbBindInfo);
        validateResourceInjectionPointType(factory, injectionPoint);

        if (factory != null) {
            return new ManagedReferenceFactoryToResourceReferenceFactoryAdapter<Object>(factory);
        } else {
            return createLazyResourceReferenceFactory(ejbBindInfo);
        }
    }

    protected void validateResourceInjectionPointType(ManagedReferenceFactory fact, InjectionPoint injectionPoint) {
        if (!(fact instanceof ContextListManagedReferenceFactory) || injectionPoint == null) {
            return; // validation is skipped as we have no information about the resource type
        }

        final ContextListManagedReferenceFactory factory = (ContextListManagedReferenceFactory) fact;
        // the resource class may come from JBoss AS
        Class<?> resourceClass = org.jboss.as.weld.util.Reflections.loadClass(factory.getInstanceClassName(), factory.getClass().getClassLoader());
        // or it may come from deployment
        if (resourceClass == null) {
            resourceClass = org.jboss.as.weld.util.Reflections.loadClass(factory.getInstanceClassName(), module.getClassLoader());
        }

        if (resourceClass != null) {
            validateResourceInjectionPointType(resourceClass, injectionPoint);
        }
        // otherwise, the validation is skipped as we have no information about the resource type
    }

    private static final Map<Class<?>, Class<?>> BOXED_TYPES;

    static {
        Map<Class<?>, Class<?>> types = new HashMap<Class<?>, Class<?>>();
        types.put(int.class, Integer.class);
        types.put(byte.class, Byte.class);
        types.put(short.class, Short.class);
        types.put(long.class, Long.class);
        types.put(char.class, Character.class);
        types.put(float.class, Float.class);
        types.put(double.class, Double.class);
        types.put(boolean.class, Boolean.class);

        BOXED_TYPES = Collections.unmodifiableMap(types);
    }

    protected static void validateResourceInjectionPointType(Class<?> resourceType, InjectionPoint injectionPoint) {
        Class<?> injectionPointRawType = Reflections.getRawType(injectionPoint.getType());
        HierarchyDiscovery discovery = new HierarchyDiscovery(resourceType);
        for (Type type : discovery.getTypeClosure()) {
            if (Reflections.getRawType(type).equals(injectionPointRawType)) {
                return;
            }
        }
        // type autoboxing
        if (resourceType.isPrimitive() && BOXED_TYPES.get(resourceType).equals(injectionPointRawType)) {
            return;
        } else if (injectionPointRawType.isPrimitive() && BOXED_TYPES.get(injectionPointRawType).equals(resourceType)) {
            return;
        }
        throw BeanLogger.LOG.invalidResourceProducerType(injectionPoint.getAnnotated(), resourceType.getName());
    }

    protected ResourceReferenceFactory<Object> createLazyResourceReferenceFactory(final ContextNames.BindInfo ejbBindInfo) {
        return new ResourceReferenceFactory<Object>() {
            @Override
            public ResourceReference<Object> createResource() {
                final ManagedReferenceFactory factory = getManagedReferenceFactory(ejbBindInfo);
                if (factory == null) {
                    return new SimpleResourceReference<>(null);
                }
                final ManagedReference instance = factory.getReference();
                return new ResourceReference<Object>() {
                    @Override
                    public Object getInstance() {
                        return instance.getInstance();
                    }

                    @Override
                    public void release() {
                        instance.release();
                    }
                };
            }
        };
    }
}

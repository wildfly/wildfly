/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.services.bootstrap;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.util.reflection.HierarchyDiscovery;
import org.jboss.weld.util.reflection.Reflections;
import org.wildfly.security.manager.WildFlySecurityManager;

public abstract class AbstractResourceInjectionServices {

    protected final ServiceRegistry serviceRegistry;
    protected final EEModuleDescription moduleDescription;

    protected AbstractResourceInjectionServices(ServiceRegistry serviceRegistry, EEModuleDescription moduleDescription) {
        this.serviceRegistry = serviceRegistry;
        this.moduleDescription = moduleDescription;
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

    protected static void validateResourceInjectionPointType(ManagedReferenceFactory fact, InjectionPoint injectionPoint) {
        if (!(fact instanceof ContextListManagedReferenceFactory) || injectionPoint == null) {
            return; // validation is skipped as we have no information about the resource type
        }

        final ContextListManagedReferenceFactory factory = (ContextListManagedReferenceFactory) fact;
        // the resource class may come from JBoss AS
        Class<?> resourceClass = org.jboss.as.weld.util.Reflections.loadClass(factory.getInstanceClassName(), factory.getClass().getClassLoader());
        // or it may come from deployment
        if (resourceClass == null) {
            resourceClass = org.jboss.as.weld.util.Reflections.loadClass(factory.getInstanceClassName(), WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
        }

        if (resourceClass != null) {
            validateResourceInjectionPointType(resourceClass, injectionPoint);
        }
        // otherwise, the validation is skipped as we have no information about the resource type
    }

    protected static void validateResourceInjectionPointType(Class<?> resourceType, InjectionPoint injectionPoint) {
        Class<?> injectionPointRawType = Reflections.getRawType(injectionPoint.getType());
        HierarchyDiscovery discovery = new HierarchyDiscovery(resourceType);
        for (Type type : discovery.getTypeClosure()) {
            if (Reflections.getRawType(type).equals(injectionPointRawType)) {
                return;
            }
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

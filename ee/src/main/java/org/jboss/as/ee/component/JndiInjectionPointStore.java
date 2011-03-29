/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Store of JNDI injections that can be applied to a component.
 *
 * @author Stuart Douglas
 */
class JndiInjectionPointStore {

    /**
     * Map of class names to injected values.
     */
    private final Map<String,List<JndiInjectedValue>> injectionPoints;
    private final Set<ServiceName> serviceNames = new HashSet<ServiceName>();

    public JndiInjectionPointStore(Map<String, List<JndiInjectedValue>> injectionPoints) {
        this.injectionPoints = new HashMap(injectionPoints);
    }

    public JndiInjectionPointStore(JndiInjectionPointStore parent) {
        this(parent.injectionPoints);
    }

    public JndiInjectionPointStore() {
        this.injectionPoints = new HashMap<String,List<JndiInjectedValue>>();
    }

    /**
     * Adds an injected value to store
     *
     * @param injectionTarget The injection target
     * @param referenceFactory The managed reference factory that creates the value
     * @param serviceName The service name that represents the managed value. May be null
     */
    public void addInjectedValue(InjectionTargetDescription injectionTarget, Value<ManagedReferenceFactory> referenceFactory, ServiceName serviceName) {
        final JndiInjectedValue value = new JndiInjectedValue(injectionTarget, referenceFactory, serviceName);
        String clazz = value.getInjectionTarget().getClassName();
        if(!injectionPoints.containsKey(clazz)) {
            injectionPoints.put(clazz,new ArrayList<JndiInjectedValue>());
        }
        injectionPoints.get(clazz).add(value);
        if(value.getServiceName() != null) {
            serviceNames.add(value.getServiceName());
        }
    }

    /**
     * Returns a list of all relevant resource injections.
     *
     * @param clazz The component class type
     * @param deploymentReflectionIndex The reflection index
     * @return
     */
    public List<ResourceInjection> applyInjections(Class<?> clazz, DeploymentReflectionIndex deploymentReflectionIndex) {
        //components need a dependency on all available JNDI entries, to make
        //sure they are accessible for programmatic lookup when the component is created
        List<ResourceInjection> resourceInjections = new ArrayList<ResourceInjection>();
        Class<?> type = clazz;
        while(type != Object.class && type != null) {
            final List<JndiInjectedValue> injectedValues = injectionPoints.get(type.getName());
            if(injectedValues != null) {
                for(final JndiInjectedValue value : injectedValues) {
                    // Create injectors for the binding
                    resourceInjections.add(ResourceInjection.Factory.create(value.getInjectionTarget(), clazz, deploymentReflectionIndex, value.getReferenceFactory()));
                }
            }
            type = type.getSuperclass();
        }
        return resourceInjections;
    }

    /**
     *
     * @return The service names of all injection points in the store.
     */
    public Set<ServiceName> getServiceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }


    /**
     * An Injection Point and the {@link org.jboss.as.naming.ManagedReferenceFactory} that creates the injected value.
     *
     * @author Stuart Douglas
     */
    private class JndiInjectedValue {

        private final InjectionTargetDescription injectionTarget;
        private final Value<ManagedReferenceFactory> referenceFactory;
        private final ServiceName serviceName;

        public JndiInjectedValue(InjectionTargetDescription injectionTarget, Value<ManagedReferenceFactory> referenceFactory, ServiceName serviceName) {
            this.referenceFactory = referenceFactory;
            this.injectionTarget = injectionTarget;
            this.serviceName = serviceName;
        }

        public InjectionTargetDescription getInjectionTarget() {
            return injectionTarget;
        }

        public Value<ManagedReferenceFactory> getReferenceFactory() {
            return referenceFactory;
        }

        public ServiceName getServiceName() {
            return serviceName;
        }
    }


}

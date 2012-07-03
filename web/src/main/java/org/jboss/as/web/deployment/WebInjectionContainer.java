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
package org.jboss.as.web.deployment;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.tomcat.InstanceManager;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.web.deployment.ConcurrentReferenceHashMap.Option;
import org.jboss.as.web.deployment.component.ComponentInstantiator;
import org.jboss.msc.service.ServiceName;

/**
 * The web injection container.
 *
 * @author Emanuel Muckenhuber
 */
public class WebInjectionContainer implements InstanceManager {

    private final ClassLoader classloader;
    private final Map<String, ComponentInstantiator> webComponentInstantiatorMap = new HashMap<String, ComponentInstantiator>();
    private final Set<ServiceName> serviceNames = new HashSet<ServiceName>();
    private final Map<Object, ManagedReference> instanceMap;

    private static final ThreadLocal<WebInjectionContainer> CURRENT_INJECTION_CONTAINER = new ThreadLocal<WebInjectionContainer>();

    public WebInjectionContainer(ClassLoader classloader) {
        this.classloader = classloader;
        this.instanceMap = new ConcurrentReferenceHashMap<Object, ManagedReference>
                (256, ConcurrentReferenceHashMap.DEFAULT_LOAD_FACTOR,
                        Runtime.getRuntime().availableProcessors(), ConcurrentReferenceHashMap.ReferenceType.STRONG,
                        ConcurrentReferenceHashMap.ReferenceType.STRONG, EnumSet.of(Option.IDENTITY_COMPARISONS));
    }

    public void addInstantiator(String className, ComponentInstantiator instantiator) {
        webComponentInstantiatorMap.put(className, instantiator);
        serviceNames.addAll(instantiator.getServiceNames());
    }

    public void destroyInstance(Object instance) throws IllegalAccessException, InvocationTargetException {
        final ManagedReference reference = instanceMap.remove(instance);
        if (reference != null) {
            reference.release();
        }
    }

    public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return newInstance(classloader.loadClass(className));
    }

    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
        final ComponentInstantiator instantiator = webComponentInstantiatorMap.get(clazz.getName());
        if (instantiator != null) {
            return instantiate(instantiator);
        }
        return clazz.newInstance();
    }

    public void newInstance(Object arg0) throws IllegalAccessException, InvocationTargetException, NamingException {
        final ComponentInstantiator instantiator = webComponentInstantiatorMap.get(arg0.getClass().getName());
        if (instantiator != null) {
            instanceMap.put(arg0, instantiator.initializeInstance(arg0));
        }
    }

    public Object newInstance(String className, ClassLoader cl) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        final ComponentInstantiator instantiator = webComponentInstantiatorMap.get(className);
        if (instantiator != null) {
            return instantiate(instantiator);
        }
        return cl.loadClass(className).newInstance();
    }

    private Object instantiate(ComponentInstantiator instantiator) {
        ManagedReference reference = instantiator.getReference();
        instanceMap.put(reference.getInstance(), reference);
        return reference.getInstance();
    }

    public Set<ServiceName> getServiceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }

    static void setWebInjectionContainer(WebInjectionContainer webInjectionContainer) {
        CURRENT_INJECTION_CONTAINER.set(webInjectionContainer);
    }

    static void clearCurrentInjectionContainer() {
        CURRENT_INJECTION_CONTAINER.remove();
    }

    public static WebInjectionContainer currentWebInjectionContainer() {
        return CURRENT_INJECTION_CONTAINER.get();
    }
}

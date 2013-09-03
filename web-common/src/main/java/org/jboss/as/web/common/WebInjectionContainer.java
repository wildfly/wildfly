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
package org.jboss.as.web.common;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Map;

import javax.naming.NamingException;

import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;

/**
 * The web injection container.
 *
 * @author Emanuel Muckenhuber
 */
public class WebInjectionContainer {

    private final ClassLoader classloader;
    private final ComponentRegistry componentRegistry;
    private final Map<Object, ManagedReference> instanceMap;

    public WebInjectionContainer(ClassLoader classloader, final ComponentRegistry componentRegistry) {
        this.classloader = classloader;
        this.componentRegistry = componentRegistry;
        this.instanceMap = new ConcurrentReferenceHashMap<Object, ManagedReference>
                (256, ConcurrentReferenceHashMap.DEFAULT_LOAD_FACTOR,
                        Runtime.getRuntime().availableProcessors(), ConcurrentReferenceHashMap.ReferenceType.STRONG,
                        ConcurrentReferenceHashMap.ReferenceType.WEAK, EnumSet.of(ConcurrentReferenceHashMap.Option.IDENTITY_COMPARISONS));
    }


    public void destroyInstance(Object instance) {
        final ManagedReference reference = instanceMap.remove(instance);
        if (reference != null) {
            reference.release();
        }
    }

    public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return newInstance(classloader.loadClass(className));
    }

    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
        final ManagedReferenceFactory factory = componentRegistry.createInstanceFactory(clazz);
        ManagedReference reference = factory.getReference();
        if (reference != null) {
            instanceMap.put(reference.getInstance(), reference);
            return reference.getInstance();
        }
        return clazz.newInstance();
    }

    public void newInstance(Object arg0) throws IllegalAccessException, InvocationTargetException, NamingException {
        final ManagedReference reference = componentRegistry.createInstance(arg0);
        if (reference != null) {
            instanceMap.put(arg0, reference);
        }
    }

    public Object newInstance(String className, ClassLoader cl) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return newInstance(cl.loadClass(className));
    }

    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }
}

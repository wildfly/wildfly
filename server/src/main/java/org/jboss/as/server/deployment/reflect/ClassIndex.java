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
package org.jboss.as.server.deployment.reflect;

import static java.lang.reflect.Modifier.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Information about a class and its methods
 *
 * @author Stuart Douglas
 */
public class ClassIndex {

    private final Class<?> moduleClass;
    private final DeploymentReflectionIndex deploymentReflectionIndex;
    private volatile Set<Method> classMethods;


    ClassIndex(final Class<?> moduleClass, final DeploymentReflectionIndex deploymentReflectionIndex) {
        this.moduleClass = moduleClass;
        this.deploymentReflectionIndex = deploymentReflectionIndex;
    }

    public Set<Method> getClassMethods() {
        if (classMethods == null) {
            synchronized (this) {
                if (classMethods == null) {
                    final Set<Method> methods = methodSet();
                    Class<?> clazz = this.moduleClass;
                    final ClassReflectionIndex<?> classIndex = deploymentReflectionIndex.getClassIndex(clazz);
                    while (clazz != null) {
                        methods.addAll(classIndex.getMethods());
                        clazz = clazz.getSuperclass();
                    }
                    final Map<Class<?>, Set<Method>> defaultMethodsByInterface = new IdentityHashMap<Class<?>, Set<Method>>();
                    clazz = this.moduleClass;
                    final Set<MethodIdentifier> foundMethods = new HashSet<>();
                    while (clazz != null) {
                        addDefaultMethods(this.moduleClass, foundMethods, defaultMethodsByInterface, clazz.getInterfaces());
                        clazz = clazz.getSuperclass();
                    }
                    for (Set<Method> methodSet : defaultMethodsByInterface.values()) {
                        methods.addAll(methodSet);
                    }
                    this.classMethods = methods;
                }
            }
        }
        return classMethods;
    }

    private boolean classContains(final Class<?> clazz, final MethodIdentifier methodIdentifier) {
        return clazz != null && (deploymentReflectionIndex.getClassIndex(clazz).getMethod(methodIdentifier) != null || classContains(clazz.getSuperclass(), methodIdentifier));
    }

    private void addDefaultMethods(final Class<?> componentClass, Set<MethodIdentifier> foundMethods, Map<Class<?>, Set<Method>> defaultMethodsByInterface, Class<?>[] interfaces) {
        for (Class<?> i : interfaces) {
            if (! defaultMethodsByInterface.containsKey(i)) {
                Set<Method> set = methodSet();
                defaultMethodsByInterface.put(i, set);
                final ClassReflectionIndex<?> interfaceIndex = deploymentReflectionIndex.getClassIndex(i);
                for (Method method : interfaceIndex.getMethods()) {
                    final MethodIdentifier identifier = MethodIdentifier.getIdentifierForMethod(method);
                    if ((method.getModifiers() & (STATIC | PUBLIC | ABSTRACT)) == PUBLIC && ! classContains(componentClass, identifier) && foundMethods.add(identifier)) {
                        set.add(method);
                    }
                }
            }
            addDefaultMethods(componentClass, foundMethods, defaultMethodsByInterface, i.getInterfaces());
        }
    }

    private static Set<Method> methodSet() {
        return Collections.newSetFromMap(new IdentityHashMap<Method, Boolean>());
    }

    public Class<?> getModuleClass() {
        return moduleClass;
    }

    @Override
    public String toString() {
        return "ClassIndex{" +
                "moduleClass=" + moduleClass + " ClassLoader " + moduleClass.getClassLoader() +
                '}';
    }
}

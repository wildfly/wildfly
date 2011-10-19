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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

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
                    final Set<Method> methods = Collections.newSetFromMap(new IdentityHashMap<Method, Boolean>());
                    Class<?> clazz = this.moduleClass;
                    while (clazz != null) {
                        methods.addAll(deploymentReflectionIndex.getClassIndex(clazz).getMethods());
                        clazz = clazz.getSuperclass();
                    }
                    this.classMethods = methods;
                }
            }
        }
        return classMethods;
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

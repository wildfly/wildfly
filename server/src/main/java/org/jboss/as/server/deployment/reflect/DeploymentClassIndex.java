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

import java.util.HashMap;
import java.util.Map;

import org.jboss.modules.Module;


/**
 * Store of class information for classes that are visible to a deployment
 * <p/>
 * This class is not threadsafe, it should not be shared across deployments
 *
 * @author Stuart Douglas
 */
public class DeploymentClassIndex {

    private final Map<String, ClassIndex> index = new HashMap<String, ClassIndex>();
    private final DeploymentReflectionIndex deploymentReflectionIndex;
    private final Module module;

    public DeploymentClassIndex(final DeploymentReflectionIndex deploymentReflectionIndex, final Module module) {
        this.deploymentReflectionIndex = deploymentReflectionIndex;
        this.module = module;
    }

    public ClassIndex classIndex(final String className) throws ClassNotFoundException {
        ClassIndex classIndex = index.get(className);
        if (classIndex == null) {
            final ClassLoader oldTccl = SecurityActions.getContextClassLoader();
            try {
                SecurityActions.setContextClassLoader(module.getClassLoader());
                final Class<?> clazz = Class.forName(className, false, module.getClassLoader());
                index.put(className, classIndex = new ClassIndex(clazz, deploymentReflectionIndex));
            } finally {
                SecurityActions.setContextClassLoader(oldTccl);
            }
        }
        return classIndex;
    }

    void cleanup() {
        index.clear();
    }
}


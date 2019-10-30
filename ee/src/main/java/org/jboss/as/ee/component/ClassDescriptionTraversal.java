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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * throwaway utility class for traversing a class configuration from most general superclass down
 */
public abstract class ClassDescriptionTraversal {
    final Class<?> clazz;
    final EEApplicationClasses applicationClasses;

    public ClassDescriptionTraversal(final Class<?> clazz, final EEApplicationClasses applicationClasses) {
        this.clazz = clazz;
        this.applicationClasses = applicationClasses;
    }

    public void run() throws DeploymentUnitProcessingException {
        Class<?> clazz = this.clazz;
        final List<EEModuleClassDescription> queue = new ArrayList<EEModuleClassDescription>();
        final List<Class<?>> classQueue = new ArrayList<Class<?>>();
        while (clazz != null && clazz != Object.class) {
            final EEModuleClassDescription configuration = applicationClasses.getClassByName(clazz.getName());
            queue.add(configuration);
            classQueue.add(clazz);
            clazz = clazz.getSuperclass();
        }
        for (int i = queue.size() - 1; i >= 0; --i) {
            final EEModuleClassDescription config = queue.get(i);
            if(config != null) {
                handle(classQueue.get(i), config);
            } else {
                handle(classQueue.get(i), null);
            }
        }
    }

    protected abstract void handle(final Class<?> clazz, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException;
}

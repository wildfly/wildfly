/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

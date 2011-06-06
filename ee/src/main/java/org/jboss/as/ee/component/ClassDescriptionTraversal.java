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

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * throwaway utility class for traversing a class configuration from most general superclass down
 */
public abstract class ClassDescriptionTraversal {
    final EEModuleClassConfiguration classConfiguration;
    final EEApplicationDescription applicationDescription;

    ClassDescriptionTraversal(final EEModuleClassConfiguration classConfiguration, final EEApplicationDescription applicationDescription) {
        this.classConfiguration = classConfiguration;
        this.applicationDescription = applicationDescription;
    }

    public void run() throws DeploymentUnitProcessingException {
        Class clazz = classConfiguration.getModuleClass();
        final Deque<EEModuleClassConfiguration> queue = new ArrayDeque<EEModuleClassConfiguration>();
        while (clazz != null && clazz != Object.class) {
            EEModuleClassConfiguration configuration = applicationDescription.getClassConfiguration(clazz.getName());
            if (configuration != null) {
                queue.addFirst(configuration);
            }
            clazz = clazz.getSuperclass();
        }
        for (EEModuleClassConfiguration configuration : queue) {
            handle(configuration, configuration.getModuleClassDescription());
        }
    }

    protected abstract void handle(final EEModuleClassConfiguration configuration, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException;
}

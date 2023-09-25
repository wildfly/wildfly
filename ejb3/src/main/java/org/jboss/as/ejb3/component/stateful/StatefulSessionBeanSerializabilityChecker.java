/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.marshalling.SerializabilityChecker;

/**
 * @author Paul Ferraro
 */
public class StatefulSessionBeanSerializabilityChecker implements SerializabilityChecker {

    private final Set<Class<?>> serializableClasses = Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

    public StatefulSessionBeanSerializabilityChecker(ModuleDeployment deployment) {
        // Find component classes of any stateful components and any superclasses
        for (EjbDeploymentInformation info: deployment.getEjbs().values()) {
            EJBComponent component = info.getEjbComponent();
            if (component instanceof StatefulSessionComponent) {
                Class<?> componentClass = component.getComponentClass();
                while (componentClass != Object.class) {
                    this.serializableClasses.add(componentClass);
                    componentClass = componentClass.getSuperclass();
                }
            }
        }
    }

    @Override
    public boolean isSerializable(Class<?> targetClass) {
        return (targetClass != Object.class) && (this.serializableClasses.contains(targetClass) || DEFAULT.isSerializable(targetClass));
    }
}

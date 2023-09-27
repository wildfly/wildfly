/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import java.util.Set;

import org.jboss.marshalling.SerializabilityChecker;

/**
 * A {@link SerializabilityChecker} based on a fixed set of classes.
 * @author Paul Ferraro
 */
public class SimpleSerializabilityChecker implements SerializabilityChecker {

    private final Set<Class<?>> serializableClasses;

    public SimpleSerializabilityChecker(Set<Class<?>> serializableClasses) {
        this.serializableClasses = serializableClasses;
    }

    @Override
    public boolean isSerializable(Class<?> targetClass) {
        return (targetClass != Object.class) && (this.serializableClasses.contains(targetClass) || DEFAULT.isSerializable(targetClass));
    }
}

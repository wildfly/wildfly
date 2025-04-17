/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;

/**
 * Factory for creating unary JNDI bindings.
 * @author Paul Ferraro
 */
public enum InfinispanCacheContainerBindingFactory implements Function<String, ContextNames.BindInfo> {
    EMBEDDED("container"),
    REMOTE("remote-container")
    ;
    private final String name;

    InfinispanCacheContainerBindingFactory(String name) {
        this.name = name;
    }

    @Override
    public ContextNames.BindInfo apply(String containerName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), this.name, containerName).getAbsoluteName());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;

/**
 * Factory for creating binary JNDI bindings.
 * @author Paul Ferraro
 */
public enum InfinispanCacheBindingFactory implements Function<BinaryServiceConfiguration, ContextNames.BindInfo> {
    CACHE("cache"),
    CACHE_CONFIGURATION("configuration")
    ;
    private final String name;

    InfinispanCacheBindingFactory(String name) {
        this.name = name;
    }

    @Override
    public ContextNames.BindInfo apply(BinaryServiceConfiguration config) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), this.name, config.getParentName(), config.getChildName()).getAbsoluteName());
    }
}

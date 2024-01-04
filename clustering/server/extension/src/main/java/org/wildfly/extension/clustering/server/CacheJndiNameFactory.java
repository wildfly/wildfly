/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.function.BiFunction;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.JndiName;

/**
 * @author Paul Ferraro
 */
public enum CacheJndiNameFactory implements BiFunction<String, String, JndiName> {
    REGISTRY_FACTORY("registry"),
    SERVICE_PROVIDER_REGISTRY("providers"),
    ;
    private final String component;

    CacheJndiNameFactory(String component) {
        this.component = component;
    }

    @Override
    public JndiName apply(String containerName, String cacheName) {
        return JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", this.component, containerName, cacheName);
    }
}

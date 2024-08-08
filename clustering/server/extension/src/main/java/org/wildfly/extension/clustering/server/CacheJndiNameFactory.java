/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.function.Function;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.JndiName;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;

/**
 * JNDI name factory for cache-based services.
 * @author Paul Ferraro
 */
public enum CacheJndiNameFactory implements Function<BinaryServiceConfiguration, JndiName> {
    REGISTRY_FACTORY("registry-factory"),
    SERVICE_PROVIDER_REGISTRY("service-provider-registrar"),
    ;
    private final String component;

    CacheJndiNameFactory(String component) {
        this.component = component;
    }

    @Override
    public JndiName apply(BinaryServiceConfiguration configuration) {
        return JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "server", this.component, configuration.getParentName(), configuration.getChildName());
    }
}

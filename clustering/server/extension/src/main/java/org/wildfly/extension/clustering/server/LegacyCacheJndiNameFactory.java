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
 * JNDI name factory for legacy cache-based clustering services.
 * @author Paul Ferraro
 */
public enum LegacyCacheJndiNameFactory implements Function<BinaryServiceConfiguration, JndiName> {
    REGISTRY_FACTORY("registry"),
    SERVICE_PROVIDER_REGISTRY("providers"),
    ;
    private final String component;

    LegacyCacheJndiNameFactory(String component) {
        this.component = component;
    }

    @Override
    public JndiName apply(BinaryServiceConfiguration configuration) {
        return JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", this.component, configuration.getParentName(), configuration.getChildName());
    }
}

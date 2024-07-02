/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;

/**
 * Factory for creating JNDI bindings.
 * @author Paul Ferraro
 */
public final class InfinispanBindingFactory {

    public static ContextNames.BindInfo createCacheContainerBinding(String containerName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "container", containerName).getAbsoluteName());
    }

    public static ContextNames.BindInfo createCacheBinding(BinaryServiceConfiguration config) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "cache", config.getParentName(), config.getChildName()).getAbsoluteName());
    }

    public static ContextNames.BindInfo createCacheConfigurationBinding(BinaryServiceConfiguration config) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "configuration", config.getParentName(), config.getChildName()).getAbsoluteName());
    }

    public static ContextNames.BindInfo createRemoteCacheContainerBinding(String remoteContainerName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "remote-container", remoteContainerName).getAbsoluteName());
    }

    private InfinispanBindingFactory() {
        // Hide
    }
}

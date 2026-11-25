/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;

/**
 * Static simplistic {@link AdditionalInitialization} implementation which registers capabilities.
 * To avoid class loading problems ensure that this is a standalone {@link Serializable} class and {@link LegacyKernelServicesInitializer#addSingleChildFirstClass(Class[])} is called.
 * Also, this class is only usable on capability-enabled containers (EAP 7.0 and newer).
 *
 * @author Radoslav Husar
 */
public class EJB3AdditionalInitialization extends AdditionalInitialization implements Serializable {

    @Serial
    private static final long serialVersionUID = 776664289594803865L;

    @Override
    protected RunningMode getRunningMode() {
        return RunningMode.ADMIN_ONLY;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
        List<String> capabilities = List.of(
                "org.wildfly.clustering.ejb.bean-management-provider.default",
                "org.wildfly.clustering.ejb.timer-management-provider.persistent",
                "org.wildfly.clustering.ejb.timer-management-provider.transient",
                "org.wildfly.clustering.infinispan.cache-configuration.ejb.default",
                "org.wildfly.clustering.infinispan.cache-container.ejb", // EAP 8.0
                "org.wildfly.clustering.infinispan.default-cache-configuration.ejb",
                "org.wildfly.ejb3.pool-config.pool",
                "org.wildfly.remoting.connector.http-remoting-connector",
                "org.wildfly.remoting.endpoint",
                "org.wildfly.threads.executor.ejb3.timer-service-thread-pool", // EAP 7.4
                "org.wildfly.transactions.global-default-local-provider"
        );

        capabilities.forEach(capabilityName -> registerCapabilities(capabilityRegistry, capabilityName));
    }

}
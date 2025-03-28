/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a cache or local registry factory.
 * @author Paul Ferraro
 */
@MetaInfServices(BinaryServiceInstallerFactory.class)
public class RegistryFactoryServiceInstallerFactory<K, V> extends AbstractRegistryFactoryServiceInstallerFactory<K, V> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        return ((configuration.getParentName() == ModelDescriptionConstants.LOCAL) ? new LocalRegistryFactoryServiceInstallerFactory<>() : new CacheRegistryFactoryServiceInstallerFactory<>()).apply(configuration);
    }
}

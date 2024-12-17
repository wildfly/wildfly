/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.clustering.infinispan.subsystem.FileStoreResourceDescription.DeprecatedAttribute;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;

/**
 * @author Paul Ferraro
 *
 */
public class FileStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<SoftIndexFileStoreConfiguration, SoftIndexFileStoreConfigurationBuilder> {

    FileStoreResourceDefinitionRegistrar() {
        super(FileStoreResourceDescription.INSTANCE);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        PathManager pathManager = context.getPathManager().orElse(null);
        if (pathManager != null) {
            ResolvePathHandler pathHandler = ResolvePathHandler.Builder.of(pathManager)
                    .setPathAttribute(DeprecatedAttribute.RELATIVE_PATH.get())
                    .setRelativeToAttribute(DeprecatedAttribute.RELATIVE_TO.get())
                    .setDeprecated(DeprecatedAttribute.RELATIVE_TO.get().getDeprecationData().getSince())
                    .build();
            registration.registerOperationHandler(pathHandler.getOperationDefinition(), pathHandler);
        }

        return registration;
    }
}

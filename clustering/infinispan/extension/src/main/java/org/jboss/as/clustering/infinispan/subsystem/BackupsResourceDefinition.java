/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ParentResourceServiceHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.PathElement;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Definition of a backups resource.
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/component=backups
 *
 * @author Paul Ferraro
 */
public class BackupsResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("backups");

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupsResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());
        ResourceServiceConfiguratorFactory serviceConfiguratorFactory = BackupsServiceConfigurator::new;
        ResourceServiceHandler handler = new ParentResourceServiceHandler(serviceConfiguratorFactory);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new BackupResourceDefinition(serviceConfiguratorFactory, this.executors).register(registration);

        return registration;
    }
}

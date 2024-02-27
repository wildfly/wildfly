/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Resource definition for mod_cluster subsystem resource, children of which are respective proxy configurations.
 *
 * @author Radoslav Husar
 */
class ModClusterSubsystemResourceDefinition extends SubsystemResourceDefinition {

    public static final PathElement PATH = pathElement(ModClusterExtension.SUBSYSTEM_NAME);

    ModClusterSubsystemResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(this);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());

        ServiceValueExecutorRegistry<ModClusterServiceMBean> registry = ServiceValueExecutorRegistry.newInstance();
        ResourceServiceHandler handler = new ModClusterSubsystemServiceHandler(registry);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new ProxyConfigurationResourceDefinition(registry).register(registration);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import static org.wildfly.extension.clustering.ejb.DistributableEjbResourceDefinition.Attribute.DEFAULT_BEAN_MANAGEMENT;
import static org.wildfly.extension.clustering.ejb.DistributableEjbResourceDefinition.DEFAULT_BEAN_MANAGEMENT_PROVIDER;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * {@link ResourceServiceHandler} for the /subsystem=distributable-ejb resource.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DistributableEjbResourceServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = DEFAULT_BEAN_MANAGEMENT.resolveModelAttribute(context, model).asString();
        return CapabilityServiceInstaller.builder(DEFAULT_BEAN_MANAGEMENT_PROVIDER, ServiceDependency.on(BeanManagementProvider.SERVICE_DESCRIPTOR, name)).build();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import static org.jboss.as.ejb3.subsystem.deployment.SingletonBeanDeploymentResourceDefinition.DEPENDS_ON;
import static org.jboss.as.ejb3.subsystem.deployment.SingletonBeanDeploymentResourceDefinition.INIT_ON_STARTUP;

import java.util.List;
import jakarta.ejb.ConcurrencyManagementType;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Handles operations that provide runtime management of a {@link SingletonComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SingletonBeanRuntimeHandler extends AbstractEJBComponentRuntimeHandler<SingletonComponent> {

    public static final SingletonBeanRuntimeHandler INSTANCE = new SingletonBeanRuntimeHandler();

    private SingletonBeanRuntimeHandler() {
        super(EJBComponentType.SINGLETON, SingletonComponent.class);
    }

    @Override
    protected void executeReadAttribute(final String attributeName, final OperationContext context, final SingletonComponent component, final PathAddress address) {
        final SingletonComponentDescription componentDescription = (SingletonComponentDescription) component.getComponentDescription();
        final ModelNode result = context.getResult();
        if (INIT_ON_STARTUP.getName().equals(attributeName)) {
            result.set(componentDescription.isInitOnStartup());
        } else if (SingletonBeanDeploymentResourceDefinition.CONCURRENCY_MANAGEMENT_TYPE.getName().equals(attributeName)) {
            final ConcurrencyManagementType concurrencyManagementType = componentDescription.getConcurrencyManagementType();
            if (concurrencyManagementType != null) {
                result.set(concurrencyManagementType.toString());
            }
        } else if (DEPENDS_ON.getName().equals(attributeName)) {
            final List<ServiceName> dependsOn = componentDescription.getDependsOn();
            for (final ServiceName dep : dependsOn) {
                final String[] nameArray = dep.toArray();
                result.add(nameArray[nameArray.length - 2]);
            }
        } else {
            super.executeReadAttribute(attributeName, context, component, address);
        }
    }
}

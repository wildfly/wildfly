/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem.deployment;

import static org.jboss.as.ejb3.subsystem.deployment.SingletonBeanDeploymentResourceDefinition.DEPENDS_ON;
import static org.jboss.as.ejb3.subsystem.deployment.SingletonBeanDeploymentResourceDefinition.INIT_ON_STARTUP;

import java.util.List;
import javax.ejb.ConcurrencyManagementType;

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

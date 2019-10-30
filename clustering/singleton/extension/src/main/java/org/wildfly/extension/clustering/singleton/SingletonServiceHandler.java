/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.SingletonResourceDefinition.Attribute.DEFAULT;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.singleton.SingletonRequirement;
import org.wildfly.extension.clustering.singleton.SingletonResourceDefinition.Capability;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class SingletonServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String defaultPolicy = DEFAULT.resolveModelAttribute(context, model).asString();
        ServiceTarget target = context.getServiceTarget();
        ServiceName serviceName = Capability.DEFAULT_POLICY.getServiceName(context.getCurrentAddress());
        ServiceName targetServiceName = SingletonServiceNameFactory.SINGLETON_POLICY.getServiceName(context, defaultPolicy);

        new IdentityServiceConfigurator<>(serviceName, targetServiceName).build(target).install();

        // Use legacy service installation for legacy capability
        ServiceName legacyServiceName = Capability.DEFAULT_LEGACY_POLICY.getServiceName(context.getCurrentAddress());

        new AliasServiceBuilder<>(legacyServiceName, targetServiceName, SingletonRequirement.SINGLETON_POLICY.getType()).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress address = context.getCurrentAddress();
        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }
    }
}

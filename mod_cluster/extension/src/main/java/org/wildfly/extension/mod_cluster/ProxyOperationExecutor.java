/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.mod_cluster;

import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ActiveServiceSupplier;

/**
 * @author Radoslav Husar
 */
public class ProxyOperationExecutor implements OperationExecutor<ModClusterServiceMBean> {

    public static final SimpleAttributeDefinition HOST = SimpleAttributeDefinitionBuilder.create("host", ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition PORT = SimpleAttributeDefinitionBuilder.create("port", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new IntRangeValidator(1, Short.MAX_VALUE - Short.MIN_VALUE, false, false))
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition VIRTUAL_HOST = SimpleAttributeDefinitionBuilder.create("virtualhost", ModelType.STRING, false)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition WAIT_TIME = SimpleAttributeDefinitionBuilder.create("waittime", ModelType.INT, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .build();

    static final SimpleAttributeDefinition CONTEXT = SimpleAttributeDefinitionBuilder.create("context", ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    static final String SESSION_DRAINING_COMPLETE = "session-draining-complete";


    @Override
    public ModelNode execute(OperationContext context, ModelNode operation, Operation<ModClusterServiceMBean> executable) throws OperationFailedException {
        ServiceName serviceName = ProxyConfigurationResourceDefinition.Capability.SERVICE.getDefinition().getCapabilityServiceName(context.getCurrentAddress());
        ModClusterServiceMBean service = new ActiveServiceSupplier<ModClusterServiceMBean>(context.getServiceRegistry(true), serviceName).get();

        return (service != null) ? executable.execute(context, operation, service) : null;
    }
}

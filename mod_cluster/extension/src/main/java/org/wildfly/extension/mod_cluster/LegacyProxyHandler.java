/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.ExceptionFunction;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyProxyHandler extends AbstractRuntimeOnlyHandler implements Registration<ManagementResourceRegistration> {

    private final FunctionExecutorRegistry<ModClusterServiceMBean> executors;
    private final Map<String, LegacyProxyOperation> operations = new HashMap<>();

    LegacyProxyHandler(FunctionExecutorRegistry<ModClusterServiceMBean> executors) {
        this.executors = executors;
        for (LegacyProxyOperation operation : EnumSet.allOf(LegacyProxyOperation.class)) {
            this.operations.put(operation.getDefinition().getName(), operation);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode op) throws OperationFailedException {
        String name = Operations.getName(op);
        LegacyProxyOperation operation = this.operations.get(name);
        PathAddress proxyAddress = LegacyMetricOperationsRegistration.translateProxyPath(context, context.getCurrentAddress());
        ServiceName serviceName = ProxyConfigurationResourceDefinition.Capability.SERVICE.getServiceName(proxyAddress);
        FunctionExecutor<ModClusterServiceMBean> executor = this.executors.get(serviceName);
        if (executor != null) {
            executor.execute(new ExceptionFunction<ModClusterServiceMBean, Void, OperationFailedException>() {
                @Override
                public Void apply(ModClusterServiceMBean service) throws OperationFailedException {
                    operation.execute(context, op, service);
                    return null;
                }
            });
        } else {
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (LegacyProxyOperation operation : this.operations.values()) {
            registration.registerOperationHandler(operation.getDefinition(), this);
        }
    }
}

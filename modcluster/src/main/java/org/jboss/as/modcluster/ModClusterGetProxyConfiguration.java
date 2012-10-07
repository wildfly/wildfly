/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.modcluster;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.net.InetSocketAddress;
import java.util.Map;

import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;

public class ModClusterGetProxyConfiguration implements OperationStepHandler {

    static final ModClusterGetProxyConfiguration INSTANCE = new ModClusterGetProxyConfiguration();

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        if (context.isNormalServer() && context.getServiceRegistry(false).getService(ModClusterService.NAME) != null) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ServiceController<?> controller = context.getServiceRegistry(false).getService(ModClusterService.NAME);
                    ModCluster modcluster = (ModCluster) controller.getValue();
                    Map<InetSocketAddress, String> map = modcluster.getProxyConfiguration();
                    ROOT_LOGGER.debugf("Mod_cluster ProxyConfiguration %s", map);
                    if (!map.isEmpty()) {
                        final ModelNode result = new ModelNode();
                        Object[] addr = map.keySet().toArray();
                        for (int i = 0; i < addr.length; i++) {
                            InetSocketAddress address = (InetSocketAddress) addr[i];
                            result.add(address.getHostName() + ":" + address.getPort());
                            result.add(map.get(addr[i]));
                        }
                        context.getResult().set(result);
                    }

                    context.stepCompleted();
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.stepCompleted();
    }
}

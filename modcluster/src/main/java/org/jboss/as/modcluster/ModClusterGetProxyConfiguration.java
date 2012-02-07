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

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;

// implements ModelQueryOperationHandler, DescriptionProvider
public class ModClusterGetProxyConfiguration implements OperationStepHandler, DescriptionProvider{

    static final ModClusterGetProxyConfiguration INSTANCE = new ModClusterGetProxyConfiguration();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getProxyConfigurationDescription(locale);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        if (context.isNormalServer()) {
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

                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep();
    }
}

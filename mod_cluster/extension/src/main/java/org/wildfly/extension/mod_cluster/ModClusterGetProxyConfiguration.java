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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import java.net.InetSocketAddress;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.wildfly.clustering.service.ActiveServiceSupplier;

public class ModClusterGetProxyConfiguration implements OperationStepHandler {

    static final ModClusterGetProxyConfiguration INSTANCE = new ModClusterGetProxyConfiguration();

    static OperationDefinition getDefinition(ResourceDescriptionResolver descriptionResolver) {
        return new SimpleOperationDefinitionBuilder(CommonAttributes.READ_PROXIES_CONFIGURATION, descriptionResolver)
                .setReadOnly()
                .setRuntimeOnly()
                .setReplyType(ModelType.LIST)
                .setReplyValueType(ModelType.STRING)
                .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                .build();
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        if (context.isNormalServer() && context.getServiceRegistry(false).getService(ContainerEventHandlerServiceConfigurator.SERVICE_NAME) != null) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ModClusterServiceMBean service = new ActiveServiceSupplier<ModClusterServiceMBean>(context.getServiceRegistry(true), ContainerEventHandlerServiceConfigurator.SERVICE_NAME).get();
                    Map<InetSocketAddress, String> map = service.getProxyConfiguration();
                    ROOT_LOGGER.debugf("Mod_cluster ProxyConfiguration %s", map);
                    if (!map.isEmpty()) {
                        final ModelNode result = new ModelNode();
                        for (Map.Entry<InetSocketAddress, String> entry : map.entrySet()) {
                            result.add(entry.getKey().getHostName() + ":" + entry.getKey().getPort());
                            if (entry.getValue() == null) {
                                result.add();
                            } else {
                                result.add(entry.getValue());
                            }
                        }
                        context.getResult().set(result);
                    }
      }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}

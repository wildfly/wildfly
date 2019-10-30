/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.wildfly.extension.picketlink.idm.service.PartitionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.Arrays;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_CONFIGURATION;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PartitionManagerRemoveHandler extends AbstractRemoveStepHandler {

    static final PartitionManagerRemoveHandler INSTANCE = new PartitionManagerRemoveHandler();

    private PartitionManagerRemoveHandler() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode partitionManagerNode)
        throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String federationName = address.getLastElement().getValue();

        removeIdentityStoreServices(context, partitionManagerNode, federationName);

        context.removeService(PartitionManagerService.createServiceName(federationName));
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    void removeIdentityStoreServices(OperationContext context, ModelNode model, String partitionManagerName, String... configurationNames) throws OperationFailedException {
        ModelNode identityConfigurationNode = model.get(IDENTITY_CONFIGURATION.getName());
        List<String> expectedConfigNames = Arrays.asList(configurationNames);

        if (identityConfigurationNode.isDefined()) {
            for (Property identityConfiguration : identityConfigurationNode.asPropertyList()) {
                String configurationName = identityConfiguration.getName();

                if (!expectedConfigNames.isEmpty() && !expectedConfigNames.contains(configurationName)) {
                    continue;
                }

                ModelNode value = identityConfiguration.getValue();

                if (value.isDefined()) {
                    for (Property store : value.asPropertyList()) {
                        context.removeService(PartitionManagerService.createIdentityStoreServiceName(partitionManagerName, configurationName, store.getName()));
                    }
                }
            }
        }
    }

    @Override
    protected boolean removeChildRecursively(PathElement child) {
        // children only represent configuration details of the parent, and are not independent entities
        return false;
    }
}

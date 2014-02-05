/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Ensures that legacy worker thread configurations and newer endpoint configurations
 * are properly used. Specifically:
 * <ol>
 *     <li>legacy worker thread pool attributes are not used on servers</li>
 *     <li>legacy worker thread pool attributes and an endpoint configuration are not both used</li>
 *     <li>adds default endpoint configuration if not present and worker thread pool attributes are not used</li>
 * </ol>
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
class WorkerThreadPoolVsEndpointHandler implements OperationStepHandler {

    static final OperationStepHandler INSTANCE = new WorkerThreadPoolVsEndpointHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ModelNode model = resource.getModel();
        Set<String> configuredAttributes = new HashSet<String>();
        for (final AttributeDefinition attribute : RemotingSubsystemRootResource.ATTRIBUTES) {
            String attrName = attribute.getName();
            if (model.hasDefined(attrName)) {
                configuredAttributes.add(attrName);
            }
        }

        Resource endpointConfig = resource.getChild(RemotingEndpointResource.ENDPOINT_PATH);
        if (configuredAttributes.size() > 0) {
            if (context.getProcessType().isServer()) {
                // worker-thread-pool not allowed on a server
                throw RemotingMessages.MESSAGES.workerConfigurationIgnored();
            } else if (endpointConfig != null) {
                // Can't configure both worker-thread-pool and endpoint
                ModelNode endpointModel = endpointConfig.getModel();
                if (endpointModel.isDefined()) {
                    for (Property prop : endpointModel.asPropertyList()) {
                        if (prop.getValue().isDefined()) {
                            throw new OperationFailedException(
                                    RemotingMessages.MESSAGES.workerThreadsEndpointConfigurationChoiceRequired(
                                            Element.WORKER_THREAD_POOL.getLocalName(), Element.ENDPOINT.getLocalName()
                                    ));
                        }
                    }
                }
            }
        } else if (endpointConfig == null) {
            // User didn't configure either worker-thread-pool or endpoint. Add a default endpoint resource so
            // users can read the default config attribute values
            context.addResource(PathAddress.pathAddress(RemotingEndpointResource.ENDPOINT_PATH), Resource.Factory.create());
        }

        context.stepCompleted();
    }
}

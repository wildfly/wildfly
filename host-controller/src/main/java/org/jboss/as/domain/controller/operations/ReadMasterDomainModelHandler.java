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

package org.jboss.as.domain.controller.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Step handler responsible for collecting a complete description of the domain model,
 * which is going to be sent back to a remote host-controller.
 *
 * @author John Bailey
 */
public class ReadMasterDomainModelHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "read-master-domain-model";
    public static final ReadMasterDomainModelHandler INSTANCE = new ReadMasterDomainModelHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Lock the model here
        final Resource root = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        // Get the list of all resources registered in this model
        context.getResult().set(describeAsNodeList(root));
        // The HC registration process will hijack the operationPrepared call and push
        // the model to a registering host-controller
        context.completeStep();
    }

    /**
     * Describe the model as a list of resources with their address and model, which
     * the HC can directly apply to create the model. Although the format might appear
     * similar as the operations generated at boot-time this description is only useful
     * to create the resource tree and cannot be used to invoke any operation.
     *
     * @param resource the root resource
     * @return the list of resources
     */
    static List<ModelNode> describeAsNodeList(final Resource resource) {
        final List<ModelNode> list = new ArrayList<ModelNode>();
        describe(PathAddress.EMPTY_ADDRESS, resource, list);
        return list;
    }

    static void describe(final PathAddress base, final Resource resource, List<ModelNode> nodes) {
        if(resource.isProxy() || resource.isRuntime()) {
            return; // ignore runtime and proxies
        } else if (base.size() >= 1 && base.getElement(0).getKey().equals(ModelDescriptionConstants.HOST)) {
            return; // ignore hosts
        }
        final ModelNode description = new ModelNode();
        description.get("domain-resource-address").set(base.toModelNode());
        description.get("domain-resource-model").set(resource.getModel());
        nodes.add(description);
        for(final String childType : resource.getChildTypes()) {
            for(final Resource.ResourceEntry entry : resource.getChildren(childType)) {
                describe(base.append(entry.getPathElement()), entry, nodes);
            }
        }
    }

    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode(); // PRIVATE operation requires no description
    }
}

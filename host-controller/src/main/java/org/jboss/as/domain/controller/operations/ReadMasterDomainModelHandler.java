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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransaction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Step handler responsible for pushing our master domain model to the remote slave
 * as part of the remote slave's registration with this master domain controller.
 *
 * @author John Bailey
 */
public class ReadMasterDomainModelHandler implements OperationStepHandler, DescriptionProvider {
    public static final String OPERATION_NAME = "read-master-domain-model";

    public static final String FORCE_DIRECT_HACK = "force-direct-hack";

    private final DomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    public ReadMasterDomainModelHandler(final DomainController domainController, final UnregisteredHostChannelRegistry registry) {
        this.domainController = domainController;
        this.registry = registry;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        //Lock the model here
        final Resource root = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final String hostName = operation.get(HOST).asString();

        // Get the list of all resources registered in this model
        final List<ModelNode> modelDescription = describeAsNodeList(root);

        ModelNode op = new ModelNode();
        op.get(OP).set(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME);
        //FIXME this makes the op work after boot (i.e. slave connects to restarted master), but does not make the slave resync the servers
        op.get(OPERATION_HEADERS, "execute-for-coordinator").set(true);
        op.get(OP_ADDR).setEmptyList();
        op.get(DOMAIN_MODEL).set(modelDescription);

        //TODO get this from somewhere
        final ProxyController proxy = registry.popChannelAndCreateProxy(hostName);

        final AtomicReference<ModelNode> failedRef = new AtomicReference<ModelNode>();
        final AtomicReference<ModelNode> preparedRef = new AtomicReference<ModelNode>();
        final AtomicReference<OperationTransaction> txRef = new AtomicReference<OperationTransaction>();
        ProxyController.ProxyOperationControl control = new ProxyController.ProxyOperationControl() {

            @Override
            public void operationFailed(ModelNode response) {
                failedRef.set(response);
            }

            @Override
            public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                txRef.set(transaction);
                preparedRef.set(result);
            }

            @Override
            public void operationCompleted(ModelNode response) {
            }
        };
        proxy.execute(op, OperationMessageHandler.logging, control, null);

        if (failedRef.get() != null) {
            final ModelNode failed = failedRef.get();
            context.getResult().set(failed.get(RESULT));
            context.getFailureDescription().set(failed.get(FAILURE_DESCRIPTION));
            context.completeStep();
        } else {
            final ModelNode preparedResult = preparedRef.get();
            context.getResult().set(preparedResult.get(RESULT));
            if (preparedResult.hasDefined(FAILURE_DESCRIPTION)) {
                context.getFailureDescription().set(preparedResult.get(FAILURE_DESCRIPTION));
            }

            OperationContext.ResultAction resultAction = context.completeStep();
            ModelController.OperationTransaction tx = txRef.get();
            if (tx != null) {
                if (resultAction == OperationContext.ResultAction.KEEP) {
                    try {
                        domainController.registerRemoteHost(proxy);
                        tx.commit();
                    } catch (SlaveRegistrationException e) {
                        context.getFailureDescription().set(e.marshal());
                        tx.rollback();
                    } catch (Exception e) {
                        context.getFailureDescription().set(SlaveRegistrationException.forUnknownError(e.getMessage()).marshal());
                        tx.rollback();
                    }
                } else {
                    tx.rollback();
                }
            }
        }
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

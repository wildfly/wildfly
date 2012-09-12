/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.operations.coordination;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.BlockingQueueOperationListener;
import org.jboss.as.controller.remote.TransactionalOperationImpl;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;

import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.controller.TransformingProxyController;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.threads.AsyncFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Emanuel Muckenhuber
 */
class HostControllerUpdateTask {

    private final String name;
    private final ModelNode operation;
    private final OperationContext context;
    private final PathAddress address;
    private final TransformingProxyController proxyController;

    public HostControllerUpdateTask(final String name, final ModelNode operation, final OperationContext context,
                                    final TransformingProxyController proxyController) {
        this.name = name;
        this.context = context;
        this.operation = operation;
        this.proxyController = proxyController;
        this.address = proxyController.getProxyNodeAddress();
    }

    public ExecutedHostRequest execute(final ProxyOperationListener listener) {
        boolean trace = HOST_CONTROLLER_LOGGER.isTraceEnabled();
        if (trace) {
            HOST_CONTROLLER_LOGGER.tracef("Sending %s to %s", operation, name);
        }
        final TransactionalProtocolClient client = proxyController.getProtocolClient();
        final OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);
        final OperationAttachments operationAttachments = new DelegatingOperationAttachments(context);
        final SubsystemInfoOperationListener subsystemListener = new SubsystemInfoOperationListener(listener, proxyController.getTransformers());
        try {

            final OperationTransformer.TransformedOperation transformationResult = proxyController.transformOperation(context, operation);
            final ModelNode transformedOperation = transformationResult.getTransformedOperation();
            final ProxyOperation proxyOperation = new ProxyOperation(name, transformedOperation, messageHandler, operationAttachments);
            try {
                // Make sure we preserve the operation headers like PrepareStepHandler.EXECUTE_FOR_COORDINATOR
                if(transformedOperation != null) {
                    transformedOperation.get(OPERATION_HEADERS).set(operation.get(OPERATION_HEADERS));
                    // If the operation was transformed in any way
                    if(operation != transformedOperation) {
                        // push all operations (incl. read-only) to the servers
                        transformedOperation.get(OPERATION_HEADERS, ServerOperationsResolverHandler.DOMAIN_PUSH_TO_SERVERS).set(true);
                    }
                }
                final AsyncFuture<ModelNode> result = client.execute(subsystemListener, proxyOperation);
                return new ExecutedHostRequest(result, transformationResult);
            } catch (IOException e) {
                // Handle protocol failures
                final TransactionalProtocolClient.PreparedOperation<ProxyOperation> result = BlockingQueueOperationListener.FailedOperation.create(proxyOperation, e);
                subsystemListener.operationPrepared(result);
                return new ExecutedHostRequest(result.getFinalResult(), transformationResult);
            }
        } catch (OperationFailedException e) {
            // Handle transformation failures
            final ProxyOperation proxyOperation = new ProxyOperation(name, operation, messageHandler, operationAttachments);
            final TransactionalProtocolClient.PreparedOperation<ProxyOperation> result = BlockingQueueOperationListener.FailedOperation.create(proxyOperation, e);
            subsystemListener.operationPrepared(result);
            return new ExecutedHostRequest(result.getFinalResult(), OperationResultTransformer.ORIGINAL_RESULT, OperationTransformer.DEFAULT_REJECTION_POLICY);
        }
    }

    static class ProxyOperation extends TransactionalOperationImpl {

        private final String name;
        protected ProxyOperation(final String name, final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            super(operation, messageHandler, attachments);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class ExecutedHostRequest implements OperationResultTransformer, OperationRejectionPolicy {

        private final AsyncFuture<ModelNode> futureResult;
        private final OperationResultTransformer resultTransformer;
        private final OperationRejectionPolicy rejectPolicy;

        ExecutedHostRequest(AsyncFuture<ModelNode> futureResult, OperationResultTransformer resultTransformer, OperationRejectionPolicy rejectPolicy) {
            this.futureResult = futureResult;
            this.resultTransformer = resultTransformer;
            this.rejectPolicy = rejectPolicy;
        }

        ExecutedHostRequest(AsyncFuture<ModelNode> futureResult, OperationTransformer.TransformedOperation transformedOperation) {
            this(futureResult, transformedOperation, transformedOperation);
        }

        public Future<ModelNode> getFinalResult() {
            return futureResult;
        }

        @Override
        public boolean rejectOperation(ModelNode preparedResult) {
            // Check the host result and see if we have to reject it
            if(preparedResult.get(RESULT).has(DOMAIN_RESULTS)) {
                final ModelNode domainResults = preparedResult.get(RESULT, DOMAIN_RESULTS);
                // Don't reject ignored operations
                if(domainResults.getType() == ModelType.STRING && IGNORED.equals(domainResults.asString())) {
                    return false;
                }
                // The format of the prepared operation of the domain coordination step1 is different from a normal operation
                // a user would need to handle, therefore try to fix it up as good as possible
                final ModelNode userOp = new ModelNode();
                userOp.get(OUTCOME).set(SUCCESS);
                userOp.get(RESULT).set(domainResults);
                return rejectPolicy.rejectOperation(userOp);
            } else {
                return rejectPolicy.rejectOperation(preparedResult);
            }
        }

        @Override
        public String getFailureDescription() {
            return rejectPolicy.getFailureDescription();
        }

        @Override
        public ModelNode transformResult(ModelNode result) {

            if(result.get(RESULT).has(DOMAIN_RESULTS)) {
                final ModelNode domainResults = result.get(RESULT, DOMAIN_RESULTS);
                if(domainResults.getType() == ModelType.STRING && IGNORED.equals(domainResults.asString())) {
                    // Untransformed
                    return result;
                }
                final ModelNode userResult = new ModelNode();
                userResult.get(OUTCOME).set(result.get(OUTCOME));
                userResult.get(RESULT).set(domainResults);
                if(result.hasDefined(FAILURE_DESCRIPTION)) {
                    userResult.get(FAILURE_DESCRIPTION).set(result.get(FAILURE_DESCRIPTION));
                }                // Transform the result
                final ModelNode transformed = resultTransformer.transformResult(userResult);
                result.get(RESULT, DOMAIN_RESULTS).set(transformed.get(RESULT));
                return result;
            } else {
                return resultTransformer.transformResult(result);
            }
        }

        public void asyncCancel() {
            futureResult.asyncCancel(false);
        }
    }

    /**
     * The transactional operation listener.
     */
    static class ProxyOperationListener extends BlockingQueueOperationListener<ProxyOperation> {
        final boolean trace = HOST_CONTROLLER_LOGGER.isTraceEnabled();

        @Override
        public void operationPrepared(final TransactionalProtocolClient.PreparedOperation<ProxyOperation> prepared) {
            try {
                super.operationPrepared(prepared);
            } finally {
                if (trace) {
                    final ModelNode result = prepared.getPreparedResult();
                    final String hostName = prepared.getOperation().getName();
                    HOST_CONTROLLER_LOGGER.tracef("Received prepared result %s from %s", result, hostName);
                }
            }
        }

        @Override
        public void operationComplete(final ProxyOperation operation, final ModelNode result) {
            try {
                super.operationComplete(operation, result);
            } finally {
                if (trace) {
                    final String hostName = operation.getName();
                    HOST_CONTROLLER_LOGGER.tracef("Received final result %s from %s", result, hostName);
                }
            }
        }
    }

    /** Checks responses from slaves for subsystem version information. TODO this is pretty hacky */
    private static class SubsystemInfoOperationListener implements TransactionalProtocolClient.TransactionalOperationListener<ProxyOperation> {

        private final ProxyOperationListener delegate;
        private final Transformers transformers;

        private SubsystemInfoOperationListener(ProxyOperationListener delegate, Transformers transformers) {
            this.delegate = delegate;
            this.transformers = transformers;
        }

        @Override
        public void operationPrepared(TransactionalProtocolClient.PreparedOperation<ProxyOperation> prepared) {
            delegate.operationPrepared(prepared);
        }

        @Override
        public void operationFailed(ProxyOperation operation, ModelNode result) {
            delegate.operationFailed(operation, result);
        }

        @Override
        public void operationComplete(ProxyOperation operation, ModelNode result) {
            try {
                storeSubsystemVersions(operation.getOperation(), result);
            } finally {
                delegate.operationComplete(operation, result);
            }
        }

        private void storeSubsystemVersions(ModelNode operation, ModelNode response) {
            PathAddress address = operation.hasDefined(OP_ADDR) ? PathAddress.pathAddress(operation.get(OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
            if (address.size() == 0 && COMPOSITE.equals(operation.get(OP).asString()) && response.hasDefined(RESULT)) {
                // recurse
                List<ModelNode> steps = operation.hasDefined(STEPS) ? operation.get(STEPS).asList() : Collections.<ModelNode>emptyList();
                ModelNode result = response.get(RESULT);
                for (int i = 0; i < steps.size(); i++) {
                    ModelNode stepOp = steps.get(i);
                    String resultID = "step-" + (i+1);
                    if (result.hasDefined(resultID)) {
                        storeSubsystemVersions(stepOp, result.get(resultID));
                    }
                }
            } else if (address.size() == 1 && ADD.equals(operation.get(OP).asString())
                        && EXTENSION.equals(address.getElement(0).getKey())
                        && response.hasDefined(RESULT) && response.get(RESULT).hasDefined(DOMAIN_RESULTS)) {
                // Extract the subsystem info and store it
                TransformationTarget target = transformers.getTarget();
                for (Property p : response.get(RESULT, DOMAIN_RESULTS).asPropertyList()) {

                    String[] version = p.getValue().asString().split("\\.");
                    int major = Integer.parseInt(version[0]);
                    int minor = Integer.parseInt(version[1]);
                    target.addSubsystemVersion(p.getName(), major, minor);
                    HOST_CONTROLLER_LOGGER.debugf("Registering subsystem %s for host %s with major version [%d] and minor version [%d]",
                            p.getName(), address, major, minor);
                }
                // purge the subsystem version data from the response
                response.get(RESULT).set(new ModelNode());
            }
        }
    }

    private static class DelegatingMessageHandler implements OperationMessageHandler {

        private final OperationContext context;
        DelegatingMessageHandler(final OperationContext context) {
            this.context = context;
        }

        @Override
        public void handleReport(MessageSeverity severity, String message) {
            context.report(severity, message);
        }
    }

    private static class DelegatingOperationAttachments implements OperationAttachments {

        private final OperationContext context;
        private DelegatingOperationAttachments(final OperationContext context) {
            this.context = context;
        }

        @Override
        public boolean isAutoCloseStreams() {
            return false;
        }

        @Override
        public List<InputStream> getInputStreams() {
            int count = context.getAttachmentStreamCount();
            List<InputStream> result = new ArrayList<InputStream>(count);
            for (int i = 0; i < count; i++) {
                result.add(context.getAttachmentStream(i));
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            //
        }
    }


}

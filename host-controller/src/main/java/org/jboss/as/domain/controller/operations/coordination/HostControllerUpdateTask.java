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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.BlockingQueueOperationListener;
import org.jboss.as.controller.remote.TransactionalOperationImpl;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;

import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.host.controller.mgmt.TransformingProxyController;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.threads.AsyncFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class HostControllerUpdateTask {

    private final String name;
    private final ModelNode operation;
    private final OperationContext context;
    private final TransactionalProtocolClient client;
    private final PathAddress address;
    private final Transformers transformers;

    public HostControllerUpdateTask(final String name, final ModelNode operation, final OperationContext context,
                                    final TransformingProxyController proxyController) {
        this.name = name;
        this.client = proxyController.getProtocolClient();
        this.context = context;
        this.operation = operation;
        this.transformers = proxyController.getTransformers();
        this.address = proxyController.getProxyNodeAddress();
    }

    public AsyncFuture<ModelNode> execute(final ProxyOperationListener listener) {
        boolean trace = HOST_CONTROLLER_LOGGER.isTraceEnabled();
        if (trace) {
            HOST_CONTROLLER_LOGGER.tracef("Sending %s to %s", operation, name);
        }
        final OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);
        final OperationAttachments operationAttachments = new DelegatingOperationAttachments(context);
        final ProxyOperation proxyOperation = new ProxyOperation(name, operation, messageHandler, operationAttachments);
        final SubsystemInfoOperationListener subsystemListener = new SubsystemInfoOperationListener(listener, transformers);
        try {
            return client.execute(subsystemListener, proxyOperation);
        } catch (IOException e) {
            final TransactionalProtocolClient.PreparedOperation<ProxyOperation> result = BlockingQueueOperationListener.FailedOperation.create(proxyOperation, e);
            subsystemListener.operationPrepared(result);
            return result.getFinalResult();
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

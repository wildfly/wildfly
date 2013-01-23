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

package org.jboss.as.controller;

import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.controller.remote.TransactionalProtocolHandlers;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ProxyController} with transformation capabilities.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformingProxyController extends ProxyController {

    /**
     * Get the underlying protocol client.
     *
     * @return the protocol client
     */
    TransactionalProtocolClient getProtocolClient();

    /**
     * Get the Transformers!
     *
     * @return the transformers
     */
    Transformers getTransformers();

    /**
     * Transform the operation.
     *
     * @param operation the operation to transform.
     * @param context the operation context
     * @return the transformed operation
     * @throws OperationFailedException
     */
    OperationTransformer.TransformedOperation transformOperation(OperationContext context, ModelNode operation) throws OperationFailedException;

    public static class Factory {

        public static TransformingProxyController create(final ManagementChannelHandler channelAssociation, final Transformers transformers, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator) {
            final TransactionalProtocolClient client = TransactionalProtocolHandlers.createClient(channelAssociation);
            return create(client, transformers, pathAddress, addressTranslator);
        }

        public static TransformingProxyController create(final TransactionalProtocolClient client, final Transformers transformers, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator) {
            final RemoteProxyController proxy = RemoteProxyController.create(client, pathAddress, addressTranslator);
            final Transformers delegating = new Transformers() {
                @Override
                public TransformationTarget getTarget() {
                    return transformers.getTarget();
                }

                @Override
                public OperationTransformer.TransformedOperation transformOperation(final TransformationContext context, final ModelNode original) throws OperationFailedException {
                    final ModelNode operation = proxy.translateOperationForProxy(original);
                    return transformers.transformOperation(context, operation);
                }

                @Override
                public Resource transformResource(ResourceTransformationContext context, Resource resource) throws OperationFailedException {
                    return transformers.transformResource(context, resource);
                }

                @Override
                public OperationTransformer.TransformedOperation transformOperation(OperationContext operationContext, ModelNode original) throws OperationFailedException {
                    final ModelNode operation = proxy.translateOperationForProxy(original);
                    return transformers.transformOperation(operationContext, operation);
                }

                @Override
                public Resource transformRootResource(OperationContext operationContext, Resource resource) throws OperationFailedException {
                    return transformers.transformRootResource(operationContext, resource);
                }
            };
            return create(proxy, delegating);
        }

        private static TransformingProxyController create(final RemoteProxyController delegate, Transformers transformers) {
            return new TransformingProxyControllerImpl(transformers, delegate);
        }

    }

    static class TransformingProxyControllerImpl implements TransformingProxyController {

        private final RemoteProxyController proxy;
        private final Transformers transformers;

        public TransformingProxyControllerImpl(Transformers transformers, RemoteProxyController proxy) {
            this.transformers = transformers;
            this.proxy = proxy;
        }

        @Override
        public TransactionalProtocolClient getProtocolClient() {
            return proxy.getTransactionalProtocolClient();
        }

        @Override
        public Transformers getTransformers() {
            return transformers;
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return proxy.getProxyNodeAddress();
        }

        @Override
        public OperationTransformer.TransformedOperation transformOperation(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            return transformers.transformOperation(context, operation);
        }

        @Override
        public void execute(final ModelNode operation, final OperationMessageHandler handler, final ProxyOperationControl control, final OperationAttachments attachments) {
            // Execute untransformed
            proxy.execute(operation, handler, control, attachments);
        }


    }

}

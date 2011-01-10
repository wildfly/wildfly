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

package org.jboss.as.controller.registry;

import java.util.Map;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * A registration for a model node which consists of a node description plus operation descriptions.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModelNodeRegistration {

    /**
     * Register the existence of an addressable sub-node of this model node.
     *
     * @param address the address of the submodel (may include a wildcard)
     * @param descriptionProvider source for descriptive information describing this
     *                            portion of the model (must not be {@code null})
     * @return a model node registration which may be used to add operations
     */
    ModelNodeRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param inherited {@code true} if the operation is inherited to child nodes, {@code false} otherwise  @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationHandler handler, DescriptionProvider descriptionProvider, boolean inherited);

    /**
     * Register a proxy model node.  The operation handler should register operations to return the description
     * of subnodes.  TODO: define those operations
     *
     * @param handler the handler to proxy all operations through for the subnode
     */
    void registerProxySubModel(PathElement address, OperationHandler handler);

    /**
     * Get the operation handler at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation handler
     */
    OperationHandler getOperationHandler(PathAddress address, String operationName);

    /**
     * Get the operation description at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation description
     */
    DescriptionProvider getOperationDescription(PathAddress address, String operationName);

    /**
     * Get the model description at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @return the model description
     */
    DescriptionProvider getModelDescription(PathAddress address);

    /**
     * Get a map of descriptions of all operations available at an address.
     *
     * @param address the address
     * @return the operation map
     */
    Map<String, DescriptionProvider> getOperationDescriptions(PathAddress address);

    /**
     * Get a complete description of this node, and optionally, all of its subnodes.
     *
     * @param recursive {@code true} if the query should be recursive, {@code false} otherwise
     * @return the description
     */
    ModelNode getNodeDescription(boolean recursive);

    /**
     * A factory for creating a new, root model node registration.
     */
    class Factory {

        private Factory() {
        }

        /**
         * Create a new root model node registration.
         *
         * @param rootModelDescriptionProvider the model description provider for the root model node
         * @return the new root model node registration
         */
        public static ModelNodeRegistration create(DescriptionProvider rootModelDescriptionProvider) {
            if (rootModelDescriptionProvider == null) {
                throw new IllegalArgumentException("rootModelDescriptionProvider is null");
            }
            return new ConcreteNodeRegistration(null, null, rootModelDescriptionProvider);
        }
    }
}

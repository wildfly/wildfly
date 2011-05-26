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
import java.util.Set;

import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.descriptions.DescriptionProvider;

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
     *
     * @throws IllegalArgumentException if a submodel is already registered at {@code address}
     */
    ModelNodeRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider);

    /**
     * Register the existence of an addressable sub-node of this model node.
     *
     * @param address the address of the submodel (may include a wildcard)
     * @param subModel registry for the submodel. Must have been created by the same {@link Factory} that
     *                 created this ModelNodeRegistration
     *
     * @throws IllegalArgumentException if a submodel is already registered at {@code address} or if
     *              {@code subModel} was created by a different {@link Factory} than the creator of
     *              this object
     */
    void registerSubModel(PathElement address, ModelNodeRegistration subModel);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, NewStepHandler handler, DescriptionProvider descriptionProvider);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param inherited {@code true} if the operation is inherited to child nodes, {@code false} otherwise
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, NewStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param inherited {@code true} if the operation is inherited to child nodes, {@code false} otherwise
     * @param entryType the operation entry type
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, NewStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType);


    /**
     * Records that the given attribute can be both read from and written to, and
     * provides operation handlers for the read and the write.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param readHandler the handler for attribute reads. May be {@code null}
     *                    in which case the default handling is used
     * @param writeHandler the handler for attribute writes. Cannot be {@code null}
     * @param storage the storage type for this attribute
     * @throws IllegalArgumentException if {@code attributeName} or {@code writeHandler} are {@code null}
     */
    void registerReadWriteAttribute(String attributeName, NewStepHandler readHandler, NewStepHandler writeHandler, AttributeAccess.Storage storage);

    /**
     * Records that the given attribute can be read from but not written to, and
     * optionally provides an operation handler for the read.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param readHandler the handler for attribute reads. May be {@code null}
     *                    in which case the default handling is used
     * @param storage the storage type for this attribute
     * @throws IllegalArgumentException if {@code attributeName} is {@code null}
     */
    void registerReadOnlyAttribute(String attributeName, NewStepHandler readHandler, AttributeAccess.Storage storage);

    /**
     * Records that the given attribute is a metric.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param metricHandler the handler for attribute reads. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code attributeName} or {@code metricHandler} are {@code null}
     */
    void registerMetric(String attributeName, NewStepHandler metricHandler);

    /**
     * Register a proxy controller.
     *
     * @param address the child of this registry that should be proxied
     * @param proxyController the proxy controller
     */
    void registerProxyController(PathElement address, ProxyController proxyController);

    /**
     * Unregister a proxy controller
     *
     * @param address the child of this registry that should no longer be proxied
     */
    void unregisterProxyController(PathElement address);

    /**
     * Get the operation handler at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation handler
     */
    NewStepHandler getOperationHandler(PathAddress address, String operationName);

    /**
     * Get the operation description at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation description
     */
    DescriptionProvider getOperationDescription(PathAddress address, String operationName);

    /**
     * Get the names of the attributes for a node
     *
     * @param address the address, relative to this node
     * @return the attribute names. If there are none an empty set is returned
     */
    Set<String> getAttributeNames(PathAddress address);

    /**
     * Gets the information on how to read from or write to the given attribute.
     *
     * @param address the address of the resource
     * @param attributeName the name of the attribute
     *
     * @return the handling information, or {@code null} if the attribute or address is unknown
     */
    AttributeAccess getAttributeAccess(PathAddress address, String attributeName);

    /**
     * Get the names of the operations for a node
     *
     * @param address the address, relative to this node
     * @return the operation names. If there are none an empty set is returned
     */
    Set<String> getChildNames(PathAddress address);

    /**
     * Gets the set of direct child address elements under the node at the passed in PathAddress
     *
     * @param address the address we want to find children for
     * @return the set of direct child elements
     */
    Set<PathElement> getChildAddresses(PathAddress address);

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
     * @param inherited true to include inherited operations
     * @return the operation map
     */
    Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited);

    /**
     * If there is a proxy controller registered under any part of the registered address it will be returned.
     * E.g. if the address passed in is <code>[a=b,c=d,e=f]</code> and there is a proxy registered under
     * <code>[a=b,c=d]</code> that proxy will be returned.
     *
     * @param address the address to look for a proxy under
     * @return the found proxy controller, or <code>null</code> if there is none
     */
    ProxyController getProxyController(final PathAddress address);

    /**
     * Finds all proxy controllers registered at the passed in address, or at lower levels.
     * <p/>
     * E.g. if the address passed in is <code>a=b</code> and there are proxies registered at
     * <code>[a=b,c=d]</code>, <code>[a=b,e=f]</code> and <code>[g-h]</code>, the proxies for
     * <code>[a=b,c=d]</code> and <code>[a=b,e=f]</code> will be returned.
     *
     * @param address the address to start looking for proxies under
     * @return the found proxy controllers, or an empty set if there are none
     */
    Set<ProxyController> getProxyControllers(PathAddress address);

    /**
     * Resolve a address against the registry.
     *
     * @param address the address
     * @return the resolved addresses
     */
    Set<PathAddress> resolveAddress(PathAddress address);

    /**
     * Get a sub model registration.
     *
     * @param address the address
     * @return the node registration, <code>null</code> if there is none
     */
    ModelNodeRegistration getSubModel(PathAddress address);

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
        public static ModelNodeRegistration create(final DescriptionProvider rootModelDescriptionProvider) {
            if (rootModelDescriptionProvider == null) {
                throw new IllegalArgumentException("rootModelDescriptionProvider is null");
            }
            return new ConcreteNodeRegistration(null, null, rootModelDescriptionProvider);
        }
    }
}

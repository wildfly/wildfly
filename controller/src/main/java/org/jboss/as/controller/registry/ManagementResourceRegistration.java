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

import java.util.EnumSet;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * A registration for a model resource which consists of a resource description plus registered operation handlers.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ManagementResourceRegistration extends ImmutableManagementResourceRegistration {



    @Override
    ManagementResourceRegistration getSubModel(PathAddress address);

    /**
     * Register the existence of an addressable sub-node of this model node. The submodel is expected to have some
     * representation in the persistent configuration model.
     *
     * @param address the address of the submodel (may include a wildcard)
     * @param descriptionProvider source for descriptive information describing this
     *                            portion of the model (must not be {@code null})
     * @return a model node registration which may be used to add operations
     *
     * @throws IllegalArgumentException if a submodel is already registered at {@code address}
     * @throws IllegalStateException if {@link #isRuntimeOnly()} returns {@code true}
     */
    ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider);

    /**
     * Register the existence of an addressable sub-node of this model node.
     *
     * @param address the address of the submodel (may include a wildcard)
     * @param subModel registry for the submodel. Must have been created by the same {@link Factory} that
     *                 created this ManagementResourceRegistration
     *
     * @throws IllegalArgumentException if a submodel is already registered at {@code address} or if
     *              {@code subModel} was created by a different {@link Factory} than the creator of
     *              this object
     */
    @Deprecated
    void registerSubModel(PathElement address, ManagementResourceRegistration subModel);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param inherited {@code true} if the operation is inherited to child nodes, {@code false} otherwise
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited);

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
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType);

    /**
     * Register an operation handler for this model node.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param inherited {@code true} if the operation is inherited to child nodes, {@code false} otherwise
     * @param entryType the operation entry type
     * @param flags operational modifier flags for this operation (e.g. read-only)
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags);


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
    void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage);

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
    void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, AttributeAccess.Storage storage);

    /**
     * Records that the given attribute is a metric.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param metricHandler the handler for attribute reads. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code attributeName} or {@code metricHandler} are {@code null}
     */
    void registerMetric(String attributeName, OperationStepHandler metricHandler);

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
        public static ManagementResourceRegistration create(final DescriptionProvider rootModelDescriptionProvider) {
            if (rootModelDescriptionProvider == null) {
                throw new IllegalArgumentException("rootModelDescriptionProvider is null");
            }
            return new ConcreteResourceRegistration(null, null, rootModelDescriptionProvider, false);
        }
    }
}

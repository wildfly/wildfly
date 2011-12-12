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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * A registration for a management resource which consists of a resource description plus registered operation handlers.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ManagementResourceRegistration extends ImmutableManagementResourceRegistration {

    /**
     * Get a sub model registration.
     * <p>This method overrides the superinterface method of the same name in order to require
     * that the returned registration be mutable.
     * </p>
     *
     * @param address the address, relative to this node
     * @return the resource registration, <code>null</code> if there is none
     */
    @Override
    ManagementResourceRegistration getSubModel(PathAddress address);

    /**
     * Register the existence of an addressable sub-resource of this resource.
     *
     * @param address the address of the submodel (may include a wildcard)
     * @param descriptionProvider source for descriptive information describing this
     *                            portion of the model (must not be {@code null})
     * @return a resource registration which may be used to add attributes, operations and sub-models
     *
     * @throws IllegalArgumentException if a submodel is already registered at {@code address}
     * @throws IllegalStateException if {@link #isRuntimeOnly()} returns {@code true}
     */
    ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider);

    /**
     * Register the existence of an addressable sub-resource of this resource. Before this method returns the provided
     * {@code resourceDefinition} will be given the opportunity to
     * {@link ResourceDefinition#registerAttributes(ManagementResourceRegistration) register attributes}
     * and {@link ResourceDefinition#registerOperations(ManagementResourceRegistration) register operations}.
     *
     * @param resourceDefinition source for descriptive information describing this
     *                            portion of the model (must not be {@code null})
     * @return a resource registration which may be used to add attributes, operations and sub-models
     *
     * @throws IllegalArgumentException if a submodel is already registered at {@code address}
     * @throws IllegalStateException if {@link #isRuntimeOnly()} returns {@code true}
     */
    ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition);

    /**
     * Unregister the existence of an addressable sub-resource of this resource.
     *
     * @param address the child of this registry that should no longer be available
     */
    void unregisterSubModel(PathElement address);

    /**
     * Gets whether this registration will always throw an exception if
     * {@link #registerOverrideModel(String, OverrideDescriptionProvider)} is invoked. An exception will always
     * be thrown for root resource registrations, {@link PathElement#WILDCARD_VALUE wildcard registrations}, or
     * {@link #isRemote() remote registrations}.
     *
     * @return {@code true} if an exception will not always be thrown; {@code false} if it will
     */
    boolean isAllowsOverride();

    /**
     * Register a specifically named resource that overrides this {@link PathElement#WILDCARD_VALUE wildcard registration}
     * by adding additional attributes, operations or child types.
     *
     * @param name the specific name of the resource. Cannot be {@code null} or {@link PathElement#WILDCARD_VALUE}
     * @param descriptionProvider provider for descriptions of the additional attributes or child types
     *
     * @return a resource registration which may be used to add attributes, operations and sub-models
     *
     * @throws IllegalArgumentException if either parameter is null or if there is already a registration under {@code name}
     * @throws IllegalStateException if {@link #isRuntimeOnly()} returns {@code true} or if {@link #isAllowsOverride()} returns false
     */
    ManagementResourceRegistration registerOverrideModel(final String name, final OverrideDescriptionProvider descriptionProvider);

    /**
     * Unregister a specifically named resource that overrides a {@link PathElement#WILDCARD_VALUE wildcard registration}
     * by adding additional attributes, operations or child types.
     *
     * @param name the specific name of the resource. Cannot be {@code null} or {@link PathElement#WILDCARD_VALUE}
     */
    void unregisterOverrideModel(final String name);

    /**
     * Register an operation handler for this resource.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider);

    /**
     * Register an operation handler for this resource.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param flags operational modifier flags for this operation (e.g. read-only)
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, EnumSet<OperationEntry.Flag> flags);

    /**
     * Register an operation handler for this resource.
     *
     * @param operationName the operation name
     * @param handler the operation handler
     * @param descriptionProvider the description provider for this operation
     * @param inherited {@code true} if the operation is inherited to child nodes, {@code false} otherwise
     * @throws IllegalArgumentException if either parameter is {@code null}
     */
    void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited);

    /**
     * Register an operation handler for this resource.
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
     * Register an operation handler for this resource.
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
     * Records that the given attribute can be both read from and written to, and
     * provides operation handlers for the read and the write. The attribute is assumed to be
     * {@link org.jboss.as.controller.registry.AttributeAccess.Storage#CONFIGURATION} unless parameter
     * {@code flags} includes {@link org.jboss.as.controller.registry.AttributeAccess.Flag#STORAGE_RUNTIME}.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param readHandler the handler for attribute reads. May be {@code null}
     *                    in which case the default handling is used
     * @param writeHandler the handler for attribute writes. Cannot be {@code null}
     * @param flags additional flags describing this attribute
     * @throws IllegalArgumentException if {@code attributeName} or {@code writeHandler} are {@code null}
     */
    void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler,
                                    EnumSet<AttributeAccess.Flag> flags);

    /**
     * Records that the given attribute can be both read from and written to, and
     * provides operation handlers for the read and the write. The attribute is assumed to be
     * {@link org.jboss.as.controller.registry.AttributeAccess.Storage#CONFIGURATION} unless parameter
     * {@code flags} includes {@link org.jboss.as.controller.registry.AttributeAccess.Flag#STORAGE_RUNTIME}.
     *
     * @param definition the attribute definition. Cannot be {@code null}
     * @param readHandler the handler for attribute reads. May be {@code null}
     *                    in which case the default handling is used
     * @param writeHandler the handler for attribute writes. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code definition} or {@code writeHandler} are {@code null}
     */
    void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler);


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
     * Records that the given attribute can be read from but not written to, and
     * optionally provides an operation handler for the read. The attribute is assumed to be
     * {@link org.jboss.as.controller.registry.AttributeAccess.Storage#CONFIGURATION} unless parameter
     * {@code flags} includes {@link org.jboss.as.controller.registry.AttributeAccess.Flag#STORAGE_RUNTIME}.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param readHandler the handler for attribute reads. May be {@code null}
     *                    in which case the default handling is used
     * @param flags additional flags describing this attribute
     * @throws IllegalArgumentException if {@code attributeName} is {@code null}
     */
    void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags);

    /**
     * Records that the given attribute can be read from but not written to, and
     * optionally provides an operation handler for the read. The attribute is assumed to be
     * {@link org.jboss.as.controller.registry.AttributeAccess.Storage#CONFIGURATION} unless parameter
     * {@code flags} includes {@link org.jboss.as.controller.registry.AttributeAccess.Flag#STORAGE_RUNTIME}.
     *
     * @param definition the attribute definition. Cannot be {@code null}
     * @param readHandler the handler for attribute reads. May be {@code null}
     *                    in which case the default handling is used
     *
     * @throws IllegalArgumentException if {@code definition} is {@code null}
     */
    void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler);

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
     * Records that the given attribute is a metric.
     *
     * @param definition the attribute definition. Cannot be {@code null}
     * @param metricHandler the handler for attribute reads. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code definition} or {@code metricHandler} are {@code null}
     */
    void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler);

    /**
     * Records that the given attribute is a metric.
     *
     * @param attributeName the name of the attribute. Cannot be {@code null}
     * @param metricHandler the handler for attribute reads. Cannot be {@code null}
     * @param flags additional flags describing this attribute
     *
     * @throws IllegalArgumentException if {@code attributeName} or {@code metricHandler} are {@code null}
     */
    void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags);

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
                throw MESSAGES.nullVar("rootModelDescriptionProvider");
            }
            ResourceDefinition rootResourceDefinition = new ResourceDefinition() {

                @Override
                public PathElement getPathElement() {
                    return null;
                }

                @Override
                public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
                    return rootModelDescriptionProvider;
                }

                @Override
                public void registerOperations(ManagementResourceRegistration resourceRegistration) {
                    //  no-op
                }

                @Override
                public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
                    //  no-op
                }
            };
            return new ConcreteResourceRegistration(null, null, rootResourceDefinition, false);
        }

        /**
         * Create a new root model node registration.
         *
         * @param resourceDefinition the facotry for the model description provider for the root model node
         * @return the new root model node registration
         */
        public static ManagementResourceRegistration create(final ResourceDefinition resourceDefinition) {
            if (resourceDefinition == null) {
                throw MESSAGES.nullVar("rootModelDescriptionProviderFactory");
            }
            return new ConcreteResourceRegistration(null, null, resourceDefinition, false);
        }
    }
}

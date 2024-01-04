/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * Registers a {@link RestartParentResourceAddStepHandler}, {@link RestartParentResourceRemoveStepHandler}, and {@link RestartParentResourceWriteAttributeHandler} on behalf of a resource definition.
 * Users of this class should ensure that the {@link ResourceServiceConfigurator} returned by the specified factory sets the appropriate initial mode of the parent service it configures.
 * Neglecting to do this may result in use of the default initial mode (i.e. ACTIVE), which would result in the parent service starting inadvertently.
 * @author Paul Ferraro
 */
public class RestartParentResourceRegistrar extends ResourceRegistrar {

    public RestartParentResourceRegistrar(ResourceServiceConfiguratorFactory parentFactory, ResourceDescriptor descriptor) {
        this(parentFactory, descriptor, null);
    }

    public RestartParentResourceRegistrar(ResourceServiceConfiguratorFactory parentFactory, ResourceDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor, new RestartParentResourceAddStepHandler(parentFactory, descriptor, handler), new RestartParentResourceRemoveStepHandler(parentFactory, descriptor, handler), new RestartParentResourceWriteAttributeHandler(parentFactory, descriptor));
    }
}

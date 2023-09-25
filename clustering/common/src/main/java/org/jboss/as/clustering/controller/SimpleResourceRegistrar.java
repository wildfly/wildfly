/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * Registers a {@link AddStepHandler}, {@link RemoveStepHandler}, and {@link WriteAttributeStepHandler} on behalf of a resource definition.
 * @author Paul Ferraro
 */
public class SimpleResourceRegistrar extends ResourceRegistrar {

    public SimpleResourceRegistrar(ResourceDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor, new AddStepHandler(descriptor, handler), new RemoveStepHandler(descriptor, handler), new WriteAttributeStepHandler(descriptor, handler));
    }
}

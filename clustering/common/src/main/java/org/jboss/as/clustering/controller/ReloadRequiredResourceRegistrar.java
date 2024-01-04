/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * @author Paul Ferraro
 */
public class ReloadRequiredResourceRegistrar extends ResourceRegistrar {

    public ReloadRequiredResourceRegistrar(AddStepHandlerDescriptor descriptor) {
        super(descriptor, new ReloadRequiredAddStepHandler(descriptor), new ReloadRequiredRemoveStepHandler(descriptor), new WriteAttributeStepHandler(descriptor));
    }
}

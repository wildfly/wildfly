/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;

/**
 * Registers a resource definition for a local provider of a client-mappings registry.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class LocalEjbClientServicesProviderResourceDefinitionRegistrar extends EjbClientServicesProviderResourceDefinitionRegistrar {

    LocalEjbClientServicesProviderResourceDefinitionRegistrar() {
        super(EjbClientServicesProviderResourceRegistration.LOCAL);
    }

    @Override
    public EjbClientServicesProvider resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return LocalEjbClientServicesProvider.INSTANCE;
    }
}

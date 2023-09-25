/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Configures a {@link org.jboss.msc.Service} using the model of a resource.
 * @author Paul Ferraro
 */
public interface ResourceServiceConfigurator extends ServiceConfigurator {

    /**
     * Configures a service using the specified operation context and model.
     * @param context an operation context, used to resolve capabilities and expressions
     * @param model the resource model
     * @return the reference to this configurator
     * @throws OperationFailedException if there was a failure reading the model or resolving expressions/capabilities
     */
    default ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return this;
    }
}

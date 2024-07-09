/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import java.util.List;

import io.undertow.server.HttpHandler;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
abstract class HandlerDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_HANDLER, true, HttpHandler.class).build();

    private final HandlerFactory factory;

    protected HandlerDefinition(PathElement path, HandlerFactory factory) {
        super(new SimpleResourceDefinition.Parameters(path, UndertowExtension.getResolver(Constants.HANDLER, path.getKey())));
        this.factory = factory;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        HandlerAdd add = new HandlerAdd(this.factory);
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(add), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(CAPABILITY);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return List.of(new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, "undertow-handler", false, false, false)));
    }
}

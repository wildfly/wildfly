/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.handlers;

import java.util.List;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
abstract class Handler extends PersistentResourceDefinition {

    static final RuntimeCapability CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_HANDLER, true, HttpHandler.class)
            //.addRuntimeOnlyRequirements(Capabilities.REF_REQUEST_CONTROLLER) -- has no function so don't use it
            .build();

    private static final List<AccessConstraintDefinition> CONSTRAINTS = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, "undertow-handler", false, false, false)
    ).wrapAsList();


    protected Handler(String name) {
        super(PathElement.pathElement(name), UndertowExtension.getResolver(Constants.HANDLER, name));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        HandlerAdd add = new HandlerAdd(this);
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(UndertowService.HANDLER, add, Handler.CAPABILITY), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        super.registerCapabilities(resourceRegistration);
        resourceRegistration.registerCapability(CAPABILITY);
    }
    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }

    abstract HttpHandler createHandler(final OperationContext context, ModelNode model) throws OperationFailedException;
}

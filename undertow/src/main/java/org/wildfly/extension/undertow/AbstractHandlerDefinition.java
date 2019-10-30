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

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public abstract class AbstractHandlerDefinition extends PersistentResourceDefinition implements Handler {

    private static final List<AccessConstraintDefinition> CONSTRAINTS = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, "undertow-filter", false, false, false)
    ).wrapAsList();

    protected AbstractHandlerDefinition(final String name, AbstractAddStepHandler addHandler, AbstractRemoveStepHandler removeHandler) {
        this(name, Constants.HANDLER, addHandler, removeHandler);
    }

    protected AbstractHandlerDefinition(final String name) {
        this(name, Constants.HANDLER);
    }

    protected AbstractHandlerDefinition(final String name, String prefix, AbstractAddStepHandler addHandler, AbstractRemoveStepHandler removeHandler) {
        super(PathElement.pathElement(name), UndertowExtension.getResolver(prefix, name), addHandler, removeHandler);
    }

    protected AbstractHandlerDefinition(final String name, String prefix) {
        super(PathElement.pathElement(name), UndertowExtension.getResolver(prefix, name));
    }

    protected AbstractHandlerDefinition(final Parameters parameters) {
        super(parameters);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD) == null) {
            registerAddOperation(resourceRegistration, new AbstractAddStepHandler(getAttributes()), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.REMOVE) == null) {
            registerRemoveOperation(resourceRegistration, new DefaultHandlerRemove(), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
    }

    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return null;
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    protected static class DefaultHandlerRemove extends AbstractRemoveStepHandler {
        private DefaultHandlerRemove() {

        }
    }

}

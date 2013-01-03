/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.BOOT_TIME;
import static org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.VALUE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.ProcessEnvironmentSystemPropertyUpdater;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * Operation handler for adding domain/host and server system properties.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyAddHandler implements OperationStepHandler{

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getOperation(ModelNode address, String value) {
        return getOperation(address, value, null);
    }

    public static ModelNode getOperation(ModelNode address, String value, Boolean boottime) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        if (value == null) {
            op.get(VALUE.getName()).set(new ModelNode());
        } else {
            op.get(VALUE.getName()).set(value);
        }
        if (boottime != null) {
            op.get(BOOT_TIME.getName()).set(boottime);
        }
        return op;
    }


    private final ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater;
    private final boolean useBoottime;
    private final AttributeDefinition[] attributes;

    /**
     * Create the SystemPropertyAddHandler
     *
     * @param systemPropertyUpdater the local process environment system property updater, or {@code null} if interaction with the process
     *                           environment is not required
     * @param useBoottime {@code true} if the system property resource should support the "boot-time" attribute
     */
    public SystemPropertyAddHandler(ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater, boolean useBoottime, AttributeDefinition[] attributes) {
        this.systemPropertyUpdater = systemPropertyUpdater;
        this.useBoottime = useBoottime;
        this.attributes = attributes;
    }


    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws  OperationFailedException {
        final ModelNode model = context.createResource(PathAddress.EMPTY_ADDRESS).getModel();
        for (AttributeDefinition attr : attributes) {
            attr.validateAndSet(operation, model);
        }

        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final String value = operation.hasDefined(VALUE.getName()) ? operation.get(VALUE.getName()).asString() : null;
        final boolean applyToRuntime = systemPropertyUpdater != null && systemPropertyUpdater.isRuntimeSystemPropertyUpdateAllowed(name, value, context.isBooting());
        final boolean reload = !applyToRuntime && context.getProcessType().isServer();

        if (applyToRuntime) {
            final String setValue = value != null ? VALUE.resolveModelAttribute(context, model).asString() : null;
            if (setValue != null) {
                SecurityActions.setSystemProperty(name, setValue);
            } else {
                SecurityActions.clearSystemProperty(name);
            }
            if (systemPropertyUpdater != null) {
                systemPropertyUpdater.systemPropertyUpdated(name, setValue);
            }
        } else if (reload) {
            context.reloadRequired();
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (reload) {
                    context.revertReloadRequired();
                }
                if (systemPropertyUpdater != null) {
                    SecurityActions.clearSystemProperty(name);
                    if (systemPropertyUpdater != null) {
                        systemPropertyUpdater.systemPropertyUpdated(name, null);
                    }
                }
            }
        });
    }
}

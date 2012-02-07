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

package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.interfaces.InetAddressUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for objects that store environment information for a process.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class ProcessEnvironment {

    /** The special process name value that triggers calculation of a UUID */
    public static final String JBOSS_DOMAIN_UUID = "jboss.domain.uuid";

    /** {@link AttributeDefinition} for the {@code name} attribute for a processes root resource */
    public static final AttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING, true)
            .setAllowExpression(true).build();

    /**
     * Gets an {@link OperationStepHandler} that can read the {@code name} attribute for a processes root resource
     * @return the handler
     */
    public OperationStepHandler getProcessNameReadHandler() {
        return new ProcessNameReadAttributeHandler();
    }

    /**
     * Gets an {@link OperationStepHandler} that can write the {@code name} attribute for a processes root resource
     * @return the handler
     */
    public OperationStepHandler getProcessNameWriteHandler() {
        return new ProcessNameWriteAttributeHandler();
    }

    /**
     * Gets the resolved name of this process; a value previously passed to {@link #setProcessName(String)} or
     * a value derived from the environment.
     *
     * @return the process name. Cannot be {@code null}
     */
    protected abstract String getProcessName();

    /**
     * Sets the process name. This method can only be called by the handler returned by
     * {@link #getProcessNameWriteHandler()}; its visibility is protected only because subclasses need to implement it.
     *
     * @param processName the process name. May be {@code null} in which case a default process name should be used.
     */
    protected abstract void setProcessName(String processName);

    /**
     * Gets whether updating the runtime system properties with the given property is allowed.
     *
     * @param propertyName  the name of the property. Cannot be {@code null}
     * @param propertyValue the value of the property. May be {@code null}
     * @param bootTime {@code true} if the process is currently booting
     *
     * @return {@code true} if the update can be applied to the runtime system properties; {@code} false if it
     *         should just be stored in the persistent configuration and the process should be put into
     *         {@link ControlledProcessState.State#RELOAD_REQUIRED reload-required state}.
     *
     * @throws OperationFailedException if a change to the given property is not allowed at all; e.g. changing
     *                                  {@code jboss.server.base.dir} after primordial boot is not allowed; the
     *                                  property can only be set from the command line
     */
    protected abstract boolean isRuntimeSystemPropertyUpdateAllowed(String propertyName,
                                                                    String propertyValue,
                                                                    boolean bootTime) throws OperationFailedException;

    /**
     * Notifies this {@code ProcessEnvironment} that the runtime value of the given system property has been updated,
     * allowing it to update any state that was originally set via the system property during primordial process boot.
     * This method should only be invoked after a call to {@link #isRuntimeSystemPropertyUpdateAllowed(String, String, boolean)}
     * has returned {@code true}.
     *
     * @param propertyName  the name of the property. Cannot be {@code null}
     * @param propertyValue the value of the property. May be {@code null}
     */
    protected abstract void systemPropertyUpdated(String propertyName, String propertyValue);

    protected static String resolveGUID(final String unresolvedName) {

        String result;

        if (JBOSS_DOMAIN_UUID.equals(unresolvedName)) {
            try {
                InetAddress localhost = InetAddressUtil.getLocalHost();
                result = UUID.nameUUIDFromBytes(localhost.getAddress()).toString();
            } catch (UnknownHostException e) {
                throw ControllerMessages.MESSAGES.cannotResolveProcessUUID(e);
            }
        } else {
            result = unresolvedName;
        }

        return result;
    }

    private class ProcessNameWriteAttributeHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();

            final ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
            final ModelNode mockOp = new ModelNode();
            mockOp.get(NAME.getName()).set(newValue);

            NAME.validateAndSet(mockOp, model);

            boolean booting = context.isBooting();
            String resolved = null;
            if (context.isBooting()) {
                final ModelNode resolvedNode = NAME.resolveModelAttribute(context, model);
                resolved = resolvedNode.isDefined() ? resolvedNode.asString() : null;
                resolved = resolved == null ? null : resolveGUID(resolved);
            } else {
                context.reloadRequired();
            }

            if (context.completeStep() == OperationContext.ResultAction.KEEP) {
                if (booting) {
                    ProcessEnvironment.this.setProcessName(resolved);
                }
            } else if (!booting) {
                context.revertReloadRequired();
            }
        }
    }

    private class ProcessNameReadAttributeHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            if (model.hasDefined(NAME.getName())) {
                context.getResult().set(model.get(NAME.getName()));
            } else {
                context.getResult().set(ProcessEnvironment.this.getProcessName());
            }

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }
}

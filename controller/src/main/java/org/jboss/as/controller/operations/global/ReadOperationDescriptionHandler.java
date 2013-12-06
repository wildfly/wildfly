/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.global;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.LOCALE;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.NAME;

import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.AuthorizationResult.Decision;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} returning the type description of a single operation description.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ReadOperationDescriptionHandler implements OperationStepHandler {

    static final SimpleAttributeDefinition ACCESS_CONTROL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ACCESS_CONTROL, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();


    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_OPERATION_DESCRIPTION_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(NAME, LOCALE, ACCESS_CONTROL)
            .setReplyType(ModelType.OBJECT)
            .setReadOnly()
            .setRuntimeOnly()
            .build();

    static final OperationStepHandler INSTANCE = new ReadOperationDescriptionHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        String operationName = NAME.resolveModelAttribute(context, operation).asString();
        boolean accessControl = ACCESS_CONTROL.resolveModelAttribute(context, operation).asBoolean();

        final DescribedOp describedOp = getDescribedOp(context, operationName, operation, !accessControl);
        if (describedOp == null || (context.getProcessType() == ProcessType.DOMAIN_SERVER && !describedOp.flags.contains(OperationEntry.Flag.RUNTIME_ONLY))) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.operationNotRegistered(operationName,
                    PathAddress.pathAddress(operation.require(OP_ADDR)))));
        } else {
            final ModelNode result = describedOp.description;
            Set<OperationEntry.Flag> flags = describedOp.flags;
            boolean readOnly = flags.contains(OperationEntry.Flag.READ_ONLY);
            result.get(READ_ONLY).set(readOnly);
            if (!readOnly) {
                if (flags.contains(OperationEntry.Flag.RESTART_ALL_SERVICES)) {
                    result.get(RESTART_REQUIRED).set("all-services");
                } else if (flags.contains(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)) {
                    result.get(RESTART_REQUIRED).set("resource-services");
                } else if (flags.contains(OperationEntry.Flag.RESTART_JVM)) {
                    result.get(RESTART_REQUIRED).set("jvm");
                }
            }

            if (accessControl) {
                final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                ModelNode operationToCheck = Util.createOperation(operationName, address);
                operationToCheck.get(OPERATION_HEADERS).set(operation.get(OPERATION_HEADERS));
                AuthorizationResult authorizationResult = context.authorizeOperation(operationToCheck);
                result.get(ACCESS_CONTROL.getName(), EXECUTE).set(authorizationResult.getDecision() == Decision.PERMIT);

            }

            context.getResult().set(result);
        }
        context.stepCompleted();
    }

    private static DescribedOp getDescribedOp(OperationContext context, String operationName, ModelNode operation, boolean lenient) throws OperationFailedException {
        DescribedOp result = null;
        ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        if (registry != null) {
            OperationEntry operationEntry = registry.getOperationEntry(PathAddress.EMPTY_ADDRESS, operationName);
            if (operationEntry != null) {
                Locale locale = GlobalOperationHandlers.getLocale(context, operation);
                result = new DescribedOp(operationEntry, locale);
            }
        } else if (lenient) {
            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            if (address.size() > 0) {
                // For wildcard elements, check specific registrations where the same OSH is used
                // for all such registrations
                PathElement pe = address.getLastElement();
                if (pe.isWildcard()) {
                    ImmutableManagementResourceRegistration rootRegistration = context.getRootResourceRegistration();
                    String type = pe.getKey();
                    PathAddress parent = address.subAddress(0, address.size() - 1);
                    Set<PathElement> children = rootRegistration.getChildAddresses(parent);
                    if (children != null) {
                        Locale locale = GlobalOperationHandlers.getLocale(context, operation);
                        DescribedOp found = null;
                        for (PathElement child : children) {
                            if (type.equals(child.getKey())) {
                                OperationEntry oe = rootRegistration.getOperationEntry(parent.append(child), operationName);
                                DescribedOp describedOp = oe == null ? null : new DescribedOp(oe, locale);
                                if (describedOp == null || (found != null && !found.equals(describedOp))) {
                                    // Not all children have the same handler; give up
                                    found = null;
                                    break;
                                }
                                // We have a candidate OSH
                                found = describedOp;
                            }
                        }
                        result = found;
                    }
                }

            }
        }
        return result;
    }

    private static class DescribedOp {
        private final ModelNode description;
        private final Set<OperationEntry.Flag> flags;

        private DescribedOp(OperationEntry operationEntry, Locale locale) {
            this.description = operationEntry.getDescriptionProvider().getModelDescription(locale);
            this.flags = operationEntry.getFlags();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DescribedOp that = (DescribedOp) o;

            return description.equals(that.description) && flags.equals(that.flags);

        }

        @Override
        public int hashCode() {
            int result = description.hashCode();
            result = 31 * result + flags.hashCode();
            return result;
        }
    }
}

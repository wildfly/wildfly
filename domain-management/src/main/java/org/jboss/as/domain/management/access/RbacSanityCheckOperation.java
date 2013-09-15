/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.AttachmentKey;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition.Provider;
import org.jboss.dmr.ModelNode;

/**
 * An {@link OperationStepHandler} to be executed at the end of stage MODEL to identify SOME situations where configuration
 * would lock out all remote access.
 *
 * Due to role mapping using information from remote stores it is not possible to exhaustively verify that users will still be
 * assigned roles after a configuration change, however there are some configuration permutations where it is guaranteed no
 * roles can be assigned regardless of the user stores and we can detect and reject those configurations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RbacSanityCheckOperation implements OperationStepHandler {

    private static final AttachmentKey<RbacSanityCheckOperation> KEY = AttachmentKey.create(RbacSanityCheckOperation.class);
    private static final RbacSanityCheckOperation INSTANCE = new RbacSanityCheckOperation();

    private RbacSanityCheckOperation() {
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        /*
         * This operation should complete successfully as soon as it is detected that there is potentially configuration that
         * will allow operations to be executed by remote users, that could mean that RBAC is disabled or at least one config
         * path is identified that potentially allows for role assignment.
         *
         * Checks to perform: -
         *   1 - Provider if not RBAC no further checking required.
         *   3 - Else iterate role definitions and verify at least one contains an include.
         *
         * Note: This operation if validating different parts of the model, although the user had access to modify one part they
         * may not have access to the remaining parts - this Operation uses a PrivilegedAction to run as an in-vm client.
         */
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                @Override
                public Void run() throws OperationFailedException {
                    final ModelChecker checker = new ModelChecker(context, context.readResource(PathAddress.EMPTY_ADDRESS));
                    if (checker.isRbacEnabled() && (checker.doRoleMappingsExist() == false)) {
                        throw MESSAGES.inconsistentRbacConfiguration();
                    }

                    context.stepCompleted();

                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            Exception cause = e.getException();
            if (cause instanceof OperationFailedException) {
                throw (OperationFailedException) cause;
            } else {
                throw new OperationFailedException(cause);
            }
        }
    }

    /**
     * Add the operation at the end of Stage MODEL if this operation has not already been registered.
     *
     * This operation should be added if any of the following occur: -
     *   - The authorization configuration is removed from a security realm.
     *   - The rbac provider is changed to rbac.
     *   - A role is removed.
     *   - An include is removed from a role.
     *   - A management interface is removed.
     *
     * Note: This list only includes actions that could invalidate the configuration, actions that would not invalidate the
     * configuration do not need this operation registering. e.g. Adding a role, if the configuration was already valid this
     * could not invalidate it.
     *
     * @param context - The OperationContext to use to register the step.
     */
    public static void addOperation(final OperationContext context) {
        RbacSanityCheckOperation added = context.getAttachment(KEY);
        if (added == null) {
            // TODO support managed domain
            if (!context.isNormalServer()) return;
            context.addStep(createOperation(), INSTANCE, Stage.MODEL);
            context.attach(KEY, INSTANCE);
        }
    }

    private static ModelNode createOperation() {
        ModelNode operation = Util.createEmptyOperation("rbac-sanity-check", PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT));

        return operation;
    }

    /**
     * A class to handle the checks on the management model.
     *
     * Performing all of the checks will require some paths through the model to be accessed multiple times, this class allows
     * for those to be reused.
     */
    private static class ModelChecker {

        private final OperationContext context;
        private final Resource managementResource;
        private ModelNode accessAuthorization;

        private ModelChecker(final OperationContext context, final Resource managementResource) {
            this.context = context;
            this.managementResource = managementResource;
        }

        boolean isRbacEnabled() throws OperationFailedException {
            ModelNode accessAuthorization = getAccessAuthorization();
            String value = AccessAuthorizationResourceDefinition.PROVIDER.resolveModelAttribute(context, accessAuthorization)
                    .asString().toUpperCase(Locale.ENGLISH);
            return Provider.valueOf(value) == Provider.RBAC;
        }


        boolean doRoleMappingsExist() throws OperationFailedException {
            Resource authorizationResource = managementResource.getChild(PathElement.pathElement(ACCESS, AUTHORIZATION));
            Set<String> roleNames = authorizationResource.getChildrenNames(ROLE_MAPPING);
            for (String current : roleNames) {
                Resource roleResource = authorizationResource.getChild(PathElement.pathElement(ROLE_MAPPING, current));
                ModelNode roleModel = roleResource.getModel();
                if (roleModel.get(INCLUDE_ALL).isDefined() && roleModel.require(INCLUDE_ALL).asBoolean()) {
                    return true;
                }

                if (roleResource.getChildren(INCLUDE).size() > 0) {
                    return true;
                }
            }

            return false;
        }

        private ModelNode getAccessAuthorization() {
            if (accessAuthorization == null) {
                PathElement pathElement = PathElement.pathElement(ACCESS, AUTHORIZATION);
                if (managementResource.hasChild(pathElement)) {
                    Resource authorizationResource = managementResource.getChild(pathElement);
                    if (authorizationResource.isModelDefined()) {
                        accessAuthorization = authorizationResource.getModel();
                    }
                }
            }

            return accessAuthorization;
        }
    }

}

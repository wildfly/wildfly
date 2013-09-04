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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.GROUPS;
import static org.jboss.as.domain.management.ModelDescriptionConstants.IDENTITY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.MAPPED_ROLES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.REALM;
import static org.jboss.as.domain.management.ModelDescriptionConstants.ROLES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.USERNAME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.WHOAMI;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.rbac.RunAsRoleMapper;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The OperationStepHandler for the whoami operation.
 *
 * The whoami operation allows for clients to request information about the currently authenticated user from the server, in
 * it's short form this will just return the username but in the verbose form this will also reveal the roles.
 *
 * This operation is needed as there are various scenarios where the client is not directly involved in the authentication
 * process from the admin console leaving the web browser to authenticate to the more silent mechanisms such as Kerberos and
 * JBoss Local User.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class WhoAmIOperation implements OperationStepHandler {

    private static final SimpleAttributeDefinition VERBOSE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VERBOSE, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(WHOAMI, ControllerResolver.getResolver("core", "management"))
            .setParameters(VERBOSE)
            .setReadOnly()
            .setReplyType(ModelType.STRING)
            .setReplyValueType(ModelType.STRING)
            .build();

    private final Authorizer authorizer;

    private WhoAmIOperation(final Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    /**
     * @see org.jboss.as.controller.OperationStepHandler#execute(org.jboss.as.controller.OperationContext,
     *      org.jboss.dmr.ModelNode)
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        boolean verbose = VERBOSE.resolveModelAttribute(context, operation).asBoolean();

        Caller caller = context.getCaller();
        if (caller == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.noSecurityContextEstablished()));
        }

        ModelNode result = context.getResult();
        ModelNode identity = result.get(IDENTITY);
        identity.get(USERNAME).set(caller.getName());
        String realm = caller.getRealm();
        if (realm != null) {
            identity.get(REALM).set(realm);
        }

        if (verbose) {
            Set<String> groupSet = caller.getAssociatedGroups();
            if (groupSet.size() > 0) {
                ModelNode groups = result.get(GROUPS);
                for (String current : groupSet) {
                    groups.add(current);
                }
            }

            Set<String> roleSet = caller.getAssociatedRoles();
            if (roleSet.size() > 0) {
                ModelNode roles = result.get(ROLES);
                for (String current : roleSet) {
                    roles.add(current);
                }
            }

            Set<String> mappedRoles = authorizer == null ? null : authorizer.getCallerRoles(context.getCaller(), context.getCallEnvironment(), RunAsRoleMapper.getOperationHeaderRoles(operation));
            if (mappedRoles != null) {
                ModelNode roles = result.get(MAPPED_ROLES);
                for (String current : mappedRoles) {
                    roles.add(current);
                }
            }
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public static OperationStepHandler createOperation(final Authorizer authorizer) {
        return new WhoAmIOperation(authorizer);
    }

}

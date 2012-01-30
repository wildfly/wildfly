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
import static org.jboss.as.domain.management.ModelDescriptionConstants.IDENTITY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.USERNAME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.REALM;
import static org.jboss.as.domain.management.ModelDescriptionConstants.ROLES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.VERBOSE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.WHOAMI;

import java.util.Locale;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.security.SecurityContext;
import org.jboss.as.domain.management.ManagementDescriptions;
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
public class WhoAmIOperation implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = WHOAMI;
    public static final WhoAmIOperation INSTANCE = new WhoAmIOperation();

    private final ParametersValidator validator;

    private WhoAmIOperation() {
        validator = new ParametersValidator();
        validator.registerValidator(VERBOSE, new ModelTypeValidator(ModelType.BOOLEAN, true));
    }

    /**
     * @see org.jboss.as.controller.OperationStepHandler#execute(org.jboss.as.controller.OperationContext,
     *      org.jboss.dmr.ModelNode)
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validator.validate(operation);

        Subject subject = SecurityActions.getSecurityContextSubject();
        if (subject == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.noSecurityContextEstablished()));
        }

        Set<RealmUser> realmUsers = subject.getPrincipals(RealmUser.class);
        if (realmUsers.size() != 1) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unexpectedNumberOfRealmUsers(realmUsers.size())));
        }


        RealmUser user = realmUsers.iterator().next();
        ModelNode result = context.getResult();
        ModelNode identity = result.get(IDENTITY);
        identity.get(USERNAME).set(user.getName());
        identity.get(REALM).set(user.getRealm());

        if (operation.hasDefined(VERBOSE) && operation.require(VERBOSE).asBoolean()) {
            ModelNode roles = result.get(ROLES);
            Set<RealmRole> roleSet = subject.getPrincipals(RealmRole.class);
            for (RealmRole current : roleSet) {
                roles.add(current.getName());
            }
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescriptions.getWhoamiOperationDescription(locale);
    }

}

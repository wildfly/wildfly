/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.management.ModelDescriptionConstants.SUBJECT;
import static org.jboss.as.domain.management.ModelDescriptionConstants.TEST;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import javax.security.auth.login.LoginException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.domain.management.SubjectIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * The {@link OperationStepHandler} to test a keytab can successfully be used to obtain a Kerberos ticket.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class KeytabTestHandler implements OperationStepHandler {

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(TEST, ControllerResolver.getResolver("core.management.security-realm.server-identity.kerberos.keytab"))
            .setReadOnly()
            .setReplyType(ModelType.STRING)
            .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Validate this is being called against an actual resource.
        // This makes valid the subsequent assumption that the relevant service will be installed.
        context.readResource(PathAddress.EMPTY_ADDRESS, false);

        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceController<KeytabService> serviceController = ManagementUtil.getKeytabService(context, operation);

                KeytabService keytabService = serviceController.getService().getValue();

                SubjectIdentity si = null;
                try {
                    si = keytabService.createSubjectIdentity(false);
                    ModelNode result = context.getResult();
                    result.get(SUBJECT).set(si.getSubject().toString());

                } catch (LoginException e) {
                    throw MESSAGES.unableToObtainTGT(e);
                } finally {
                    if (si != null) {
                        si.logout();
                    }
                }

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, Stage.RUNTIME);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}

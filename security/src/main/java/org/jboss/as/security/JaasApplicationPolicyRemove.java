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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.security.config.ApplicationPolicyRegistration;

/**
 * Add JAAS Application Policy Operation.
 *
 * @author Brian Stansberry
 */
class JaasApplicationPolicyRemove implements ModelAddOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    static final String OPERATION_NAME = REMOVE;

    static final JaasApplicationPolicyRemove INSTANCE = new JaasApplicationPolicyRemove();

    /** Private to ensure a singleton. */
    private JaasApplicationPolicyRemove() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SecuritySubsystemDescriptions.getJaasApplicationPolicyRemove(locale);
    }

    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        ModelNode opAddr = operation.require(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String policyName = address.getLastElement().getValue();

        // Create the compensating operation
        final ModelNode compensatingOperation = JaasApplicationPolicyAdd.getRecreateOperation(opAddr, context.getSubModel());

        if (context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext updateContext = (RuntimeOperationContext) context;
         // remove jaas configuration service
            final ServiceController<?> jaasConfigurationService = updateContext.getServiceRegistry().getService(
                    JaasConfigurationService.SERVICE_NAME);
            if (jaasConfigurationService != null) {
                ApplicationPolicyRegistration config = (ApplicationPolicyRegistration) jaasConfigurationService.getValue();
                config.removeApplicationPolicy(policyName);
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }
}

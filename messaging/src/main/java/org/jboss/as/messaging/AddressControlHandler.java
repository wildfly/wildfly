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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.messaging.ManagementUtil.reportListOfString;
import static org.jboss.as.messaging.ManagementUtil.reportRoles;
import static org.jboss.as.messaging.ManagementUtil.reportRolesAsJSON;
import static org.jboss.as.messaging.CommonAttributes.*;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.EnumSet;
import java.util.Locale;

import org.hornetq.api.core.management.AddressControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handles operations and attribute reads supported by a HornetQ {@link org.hornetq.api.core.management.AddressControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AddressControlHandler extends AbstractRuntimeOnlyHandler implements DescriptionProvider {

    public static AddressControlHandler INSTANCE = new AddressControlHandler();

    public static final String[] ATTRIBUTES = { ROLES_ATTR_NAME, QUEUE_NAMES, NUMBER_OF_BYTES_PER_PAGE, NUMBER_OF_PAGES, BINDING_NAMES };
    // we keep the operation for backwards compatibility but it duplicates the "roles" attributes
    @Deprecated
    public static final String GET_ROLES_AS_JSON = "get-roles-as-json";

    private AddressControlHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String operationName = operation.require(OP).asString();
        if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
            handleReadAttribute(context, operation);
        } else if (GET_ROLES_AS_JSON.equals(operationName)) {
            handleGetRolesAsJson(context, operation);
        }
    }

    public void register(final ManagementResourceRegistration registry) {

        for (String attr : ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, this, AttributeAccess.Storage.RUNTIME);
        }

        registry.registerOperationHandler(GET_ROLES_AS_JSON, this, this, EnumSet.of(OperationEntry.Flag.READ_ONLY));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, AddressControlHandler.GET_ROLES_AS_JSON,
                CommonAttributes.CORE_ADDRESS, ModelType.STRING, true);
    }

    private void handleReadAttribute(OperationContext context, ModelNode operation) {
        final AddressControl addressControl = getAddressControl(context, operation);
        if (addressControl == null) {
            ManagementUtil.rollbackOperationWithNoHandler(context, operation);
            return;
        }

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if (ROLES_ATTR_NAME.equals(name)) {
                String json = addressControl.getRolesAsJSON();
                reportRoles(context, json);
            } else if (QUEUE_NAMES.equals(name)) {
                String[] queues = addressControl.getQueueNames();
                reportListOfString(context, queues);
            } else if (NUMBER_OF_BYTES_PER_PAGE.equals(name)) {
                long l = addressControl.getNumberOfBytesPerPage();
                context.getResult().set(l);
            } else if (NUMBER_OF_PAGES.equals(name)) {
                int i = addressControl.getNumberOfPages();
                context.getResult().set(i);
            } else if (BINDING_NAMES.equals(name)) {
                String[] bindings = addressControl.getBindingNames();
                reportListOfString(context, bindings);
            } else {
                // Bug
                throw MESSAGES.unsupportedAttribute(name);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }

        context.completeStep();
    }

    private void handleGetRolesAsJson(final OperationContext context, final ModelNode operation) {
        final AddressControl addressControl = getAddressControl(context, operation);
        try {
            String json = addressControl.getRolesAsJSON();
            reportRolesAsJSON(context, json);
            context.completeStep();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
    }

    private AddressControl getAddressControl(final OperationContext context, final ModelNode operation) {
        final String addressName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        return AddressControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.CORE_ADDRESS + addressName));
    }
}

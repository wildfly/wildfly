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

package org.jboss.as.domain.management.connections.ldap;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.net.UnknownHostException;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for removing ldap management connections.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LdapConnectionRemoveHandler extends AbstractRemoveStepHandler {

    public static final LdapConnectionRemoveHandler INSTANCE = new LdapConnectionRemoveHandler();

    private LdapConnectionRemoveHandler() {
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        ServiceName svcName = LdapConnectionManagerService.BASE_SERVICE_NAME.append(name);
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> controller = registry.getService(svcName);
        boolean removeIt;
        ServiceController.Substate substate = controller == null ? null : controller.getSubstate();
        if (substate != null && substate.getState() == ServiceController.State.UP && substate.isRestState()) {
            removeIt = operation.hasDefined(OPERATION_HEADERS)
                    && operation.get(OPERATION_HEADERS).hasDefined(ALLOW_RESOURCE_SERVICE_RESTART)
                    && operation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).asBoolean();
        } else {
            removeIt = true;
        }

        if (removeIt) {
            context.removeService(svcName);
        } else {
            context.reloadRequired();
        }
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        ServiceName svcName = LdapConnectionManagerService.BASE_SERVICE_NAME.append(name);
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> controller = registry.getService(svcName);
        if (controller != null) {
            // We didn't remove it, we just set reloadRequired
            context.revertReloadRequired();
        } else {
            LdapConnectionAddHandler.INSTANCE.performRuntime(context, operation, model, null, null);
        }
    }
}

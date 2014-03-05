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

package org.jboss.as.host.controller.operations;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.remoting.RemotingHttpUpgradeService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.dmr.ModelNode;

/**
 * Removes the HTTP management interface.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HttpManagementRemoveHandler extends AbstractRemoveStepHandler {

    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final HostControllerEnvironment environment;

    public HttpManagementRemoveHandler(final LocalHostControllerInfoImpl hostControllerInfo, final HostControllerEnvironment environment) {
        this.hostControllerInfo = hostControllerInfo;
        this.environment = environment;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.removeService(UndertowHttpManagementService.SERVICE_NAME);

        RemotingServices.removeConnectorServices(context, ManagementRemotingServices.HTTP_CONNECTOR);
        context.removeService(RemotingHttpUpgradeService.UPGRADE_SERVICE_NAME.append(ManagementRemotingServices.HTTP_CONNECTOR));

        clearHostControllerInfo(hostControllerInfo);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        HttpManagementAddHandler.populateHostControllerInfo(hostControllerInfo, context, model);
        boolean httpUpgrade = HttpManagementResourceDefinition.HTTP_UPGRADE_ENABLED.resolveModelAttribute(context, model).asBoolean();
        HttpManagementAddHandler.installHttpManagementServices(context.getRunningMode(), context.getServiceTarget(), hostControllerInfo, environment, null, false, httpUpgrade, context.getServiceRegistry(false), null);
    }

    static void clearHostControllerInfo(LocalHostControllerInfoImpl hostControllerInfo) {
        hostControllerInfo.setHttpManagementInterface(null);
        hostControllerInfo.setHttpManagementPort(0);
        hostControllerInfo.setHttpManagementSecureInterface(null);
        hostControllerInfo.setHttpManagementSecurePort(0);
        hostControllerInfo.setHttpManagementSecurityRealm(null);
    }
}

/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostAdd extends AbstractAddStepHandler {

    static final HostAdd INSTANCE = new HostAdd();

    private HostAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        HostDefinition.ALIAS.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress parent = address.subAddress(0, address.size() - 1);
        final String name = address.getLastElement().getValue();
        List<String> aliases = HostDefinition.ALIAS.unwrap(context, model);
        final String serverName = parent.getLastElement().getValue();
        final ServiceName virtualHostServiceName = UndertowService.virtualHostName(serverName, name);
        Host service = new Host(name, aliases == null ? new LinkedList<String>() : aliases);
        final ServiceBuilder<Host> builder = context.getServiceTarget().addService(virtualHostServiceName, service)
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServer())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowService())
                .addAliases(WebHost.SERVICE_NAME.append(name));

        if (aliases != null) {
            for (String alias : aliases) {
                builder.addAliases(WebHost.SERVICE_NAME.append(alias));
            }
        }

        builder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<Host> serviceController = builder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }
}

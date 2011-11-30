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

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for adding a fully specified interface.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HostSpecifiedInterfaceAddHandler extends SpecifiedInterfaceAddHandler {
    private static Logger log = Logger.getLogger("org.jboss.as.host.controller");

    final LocalHostControllerInfoImpl hostControllerInfo;

    public HostSpecifiedInterfaceAddHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        super();
        this.hostControllerInfo = hostControllerInfo;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, String name, ParsedInterfaceCriteria criteria) {
        super.performRuntime(context, operation, model, verificationHandler, newControllers, name, criteria);
        hostControllerInfo.addNetworkInterfaceBinding(name, criteria);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType() == ProcessType.HOST_CONTROLLER;
    }
}

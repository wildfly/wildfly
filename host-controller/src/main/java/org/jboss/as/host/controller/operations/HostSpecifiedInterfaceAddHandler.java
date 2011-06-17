/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;

import java.util.List;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
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
    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, String name, ParsedInterfaceCriteria criteria) {
        super.performRuntime(context, operation, model, verificationHandler, newControllers, name, criteria);
        hostControllerInfo.addNetworkInterfaceBinding(name, criteria);
    }

    @Override
    protected boolean requiresRuntime(NewOperationContext context) {
        return true;
    }
}

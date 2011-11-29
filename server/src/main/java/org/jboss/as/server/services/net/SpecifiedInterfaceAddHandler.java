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
package org.jboss.as.server.services.net;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding a fully specified interface.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SpecifiedInterfaceAddHandler extends InterfaceAddHandler {

    public static SpecifiedInterfaceAddHandler INSTANCE = new SpecifiedInterfaceAddHandler();

    protected SpecifiedInterfaceAddHandler() {
        super(true);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, String name, ParsedInterfaceCriteria criteria) {
        final ServiceTarget target = context.getServiceTarget();
        ServiceBuilder<NetworkInterfaceBinding> builder = target.addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(name), createInterfaceService(name, criteria));
        newControllers.add(builder.setInitialMode(Mode.ON_DEMAND)
                .install());
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }

    /**
     * Create a {@link NetworkInterfaceService}.
     *
     * @return the interface service
     */
    public static Service<NetworkInterfaceBinding> createInterfaceService(String name, ParsedInterfaceCriteria criteria) {
        return NetworkInterfaceService.create(name, criteria);
    }


}

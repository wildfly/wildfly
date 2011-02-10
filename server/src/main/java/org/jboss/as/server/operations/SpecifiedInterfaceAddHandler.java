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
package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding a fully specified interface.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SpecifiedInterfaceAddHandler extends InterfaceAddHandler implements RuntimeOperationHandler {

    public static SpecifiedInterfaceAddHandler INSTANCE = new SpecifiedInterfaceAddHandler();

    private SpecifiedInterfaceAddHandler() {
        super(true);
    }

    @Override
    protected void installInterface(String name, ParsedInterfaceCriteria criteria, OperationContext context, ResultHandler resultHandler, ModelNode compensatingOp) {
        if (context instanceof RuntimeOperationContext) {
            RuntimeOperationContext runtimeContext = (RuntimeOperationContext) context;
            final ServiceTarget target = runtimeContext.getServiceTarget();
            ServiceBuilder<NetworkInterfaceBinding> builder = target.addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(name), createInterfaceService(name, criteria));
            // This doesn't work -- the service is ON_DEMAND so there will be no callback, and the model will not get updated
//                .addListener(new ResultHandler.ServiceStartListener(resultHandler, compensatingOp))
             builder.setInitialMode(Mode.ON_DEMAND)
                .install();
        }
//        else {
            resultHandler.handleResultComplete(compensatingOp);
//        }
    }

    /**
     * Create a {@link NetworkInterfaceService}.
     *
     * @return the interface service
     */
    Service<NetworkInterfaceBinding> createInterfaceService(String name, ParsedInterfaceCriteria criteria) {
        return NetworkInterfaceService.create(name, criteria);
    }


}

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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.host.controller.HostOperationContext;
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
        if (context instanceof HostOperationContext) {
            HostOperationContext runtimeContext = (HostOperationContext) context;
            final ServiceTarget target = runtimeContext.getServiceTarget();
            ServiceBuilder<NetworkInterfaceBinding> builder = target.addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(name), createInterfaceService(name, criteria));
            // This doesn't work -- the service is ON_DEMAND so there will be no callback, and the model will not get updated
//                .addListener(new ResultHandler.ServiceStartListener(resultHandler, compensatingOp))
             builder.setInitialMode(Mode.ON_DEMAND)
                .install();
        }
        resultHandler.handleResultComplete(compensatingOp);
    }

    /**
     * Create a {@link NetworkInterfaceService}.
     *
     * @return the interface service
     */
    Service<NetworkInterfaceBinding> createInterfaceService(String name, ParsedInterfaceCriteria criteria) {
        return new NetworkInterfaceService(name, criteria.isAnyLocalV4(), criteria.isAnyLocalV6(), criteria.isAnyLocal(), new OverallInterfaceCriteria(criteria.getCriteria()));
    }

    /** Overall interface criteria. */
    static final class OverallInterfaceCriteria implements InterfaceCriteria {
        private static final long serialVersionUID = -5417786897309925997L;
        private final Set<InterfaceCriteria> interfaceCriteria;

        public OverallInterfaceCriteria(Set<InterfaceCriteria> criteria) {
            interfaceCriteria = criteria;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
            for (InterfaceCriteria criteria : interfaceCriteria) {
                if (! criteria.isAcceptable(networkInterface, address))
                    return false;
            }
            return true;
        }
    }
}

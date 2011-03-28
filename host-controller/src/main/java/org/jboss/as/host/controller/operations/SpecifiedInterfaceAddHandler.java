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
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding a fully specified interface.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SpecifiedInterfaceAddHandler extends InterfaceAddHandler {
    private static Logger log = Logger.getLogger("org.jboss.as.host.controller");

    public static SpecifiedInterfaceAddHandler INSTANCE = new SpecifiedInterfaceAddHandler();

    private SpecifiedInterfaceAddHandler() {
        super(true);
    }

    @Override
    protected OperationResult installInterface(final String name, final ParsedInterfaceCriteria criteria, final OperationContext context, final ResultHandler resultHandler, final ModelNode compensatingOp) {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget target = context.getServiceTarget();
                    ServiceBuilder<NetworkInterfaceBinding> builder = target.addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(name), createInterfaceService(name, criteria));
                    builder.setInitialMode(Mode.ON_DEMAND)
                            .install();
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOp);
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

        /**
         * Loop through associated criteria and build a unique address as follows:
         * 1. Iterate through all criteria, returning null if any criteria return a null result
         * from {@linkplain #isAcceptable(java.net.NetworkInterface, java.net.InetAddress)}.
         * 2. Collect the accepted addressed into a Set.
         * 3. If the set contains a single address, this is returned as the criteria match.
         * 4. If there are more than 2 addresses, log a warning and return null to indicate no
         *  match was agreed on.
         * 5. If there are 2 addresses, remove the input address and if the resulting set
         *  has only one address, return it as the criteria match. Otherwise, log a warning
         *  indicating 2 unique criteria addresses were seen and return null to indicate no
         *  match was agreed on.
         * @return the unique address determined by the all criteria, null if no such address
         * was found
         * @throws SocketException
         */
        @Override
        public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
            // Build up a set of unique addresses from the criteria
            HashSet<InetAddress> addresses = new HashSet<InetAddress>();
            for (InterfaceCriteria criteria : interfaceCriteria) {
                InetAddress bindAddress = criteria.isAcceptable(networkInterface, address);
                if (bindAddress == null)
                    return null;
                log.tracef("%s accepted, provided address: %s", criteria, bindAddress);
                addresses.add(bindAddress);
            }
            // Determine which address to return
            InetAddress bindAddress = null;
            if(addresses.size() > 0) {
                if(addresses.size() == 1)
                    bindAddress = addresses.iterator().next();
                else {
                    // Remove the input address and see if non-unique addresses exist
                    if(addresses.size() > 2)
                        log.warnf("More than two unique criteria addresses were seen: %s\n", addresses.toString());
                    else {
                        addresses.remove(address);
                        if(addresses.size() == 1)
                            bindAddress = addresses.iterator().next();
                        else
                            log.warnf("Two unique criteria addresses were seen: %s\n", addresses.toString());
                    }
                }
            }
            return bindAddress;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder("OverallInterfaceCriteria(");
            for (InterfaceCriteria criteria : interfaceCriteria) {
                sb.append(criteria.toString());
                sb.append(",");
            }
            sb.setLength(sb.length()-1);
            sb.append(")");
            return sb.toString();
        }
    }
}

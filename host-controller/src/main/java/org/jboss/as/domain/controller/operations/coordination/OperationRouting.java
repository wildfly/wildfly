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

package org.jboss.as.domain.controller.operations.coordination;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates routing information for an operation executed against a host controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationRouting {

    public static OperationRouting determineRouting(final ModelNode operation, final LocalHostControllerInfo localHostControllerInfo,
                                                    final ImmutableManagementResourceRegistration registry)
                                                        throws OperationFailedException {

        checkNull(operation, registry);

        OperationRouting routing = null;

        String targetHost = null;
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String operationName = operation.get(OP).asString();
        boolean compositeOp = false;
        if (address.size() > 0) {
            PathElement first = address.getElement(0);
            if (HOST.equals(first.getKey())) {
                targetHost = first.getValue();
            }
        } else {
            compositeOp = COMPOSITE.equals(operationName);
        }

        if (targetHost != null) {
            Set<OperationEntry.Flag> flags = registry.getOperationFlags(PathAddress.EMPTY_ADDRESS, operationName);
            checkNull(operation, flags);
            if(flags.contains(OperationEntry.Flag.READ_ONLY) && !flags.contains(OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS)) {
                routing =  new OperationRouting(targetHost, false);
            }
            // Check if the target is an actual server
            else if(address.size() > 1) {
                PathElement first = address.getElement(1);
                if (SERVER.equals(first.getKey())) {
                    routing =  new OperationRouting(targetHost, false);
                }
            }
            if (routing == null) {
                if(flags.contains(OperationEntry.Flag.HOST_CONTROLLER_ONLY)) {
                    routing = new OperationRouting(targetHost, false);
                } else {
                    routing = new OperationRouting(targetHost, true);
                }
            }
        } else if (compositeOp) {
            // Recurse into the steps to see what's required
            if (operation.hasDefined(STEPS)) {
                Set<String> allHosts = new HashSet<String>();
                boolean twoStep = false;
                for (ModelNode step : operation.get(STEPS).asList()) {
                    ImmutableManagementResourceRegistration stepRegistry = registry.getSubModel(PathAddress.pathAddress(step.get(OP_ADDR)));
                    OperationRouting stepRouting = determineRouting(step, localHostControllerInfo, stepRegistry);
                    if (stepRouting.isTwoStep()) {
                        twoStep = true;
                    }
                    allHosts.addAll(stepRouting.getHosts());
                }

                // AS7-2907 Always process as a two-step so the results for a composite op are consistent in all cases
                // routing = new OperationRouting(allHosts.iterator().next(), twoStep);
//                if (allHosts.size() == 1) {
//                    routing = new OperationRouting(allHosts.iterator().next(), true);
//                }
//                else {
//                    routing = new OperationRouting(allHosts);
//                }
                routing = new OperationRouting(allHosts);
            }
            else {
                // empty; this will be an error but don't deal with it here
                // Let our DomainModel deal with it
                routing = new OperationRouting(localHostControllerInfo.getLocalHostName(), false);
            }
        } else {
            // Domain level operation
            Set<OperationEntry.Flag> flags = registry.getOperationFlags(PathAddress.EMPTY_ADDRESS, operationName);
            checkNull(operation, flags);
            if (flags.contains(OperationEntry.Flag.READ_ONLY) && !flags.contains(OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS)) {
                // Direct read of domain model
                routing = new OperationRouting(localHostControllerInfo.getLocalHostName(), false);
            } else if (!localHostControllerInfo.isMasterDomainController()) {
                // Route to master
                routing = new OperationRouting();
            } else if (flags.contains(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY)) {
                // Deployment ops should be executed on the master DC only
                routing = new OperationRouting(localHostControllerInfo.getLocalHostName(), false);
            }
        }

        if (routing == null) {
            // Write operation to the model or a read that needs to be pushed to servers; everyone gets it
            routing = new OperationRouting(true);
        }
        return routing;

    }

    private static void checkNull(final ModelNode operation, final Object toCheck) throws OperationFailedException {
        if (toCheck == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.noHandlerForOperation( operation.require(OP).asString(),
                    PathAddress.pathAddress(operation.get(OP_ADDR)))));
        }

    }

    private final String singleHost;
    private final Set<String> hosts = new HashSet<String>();
    private final boolean twoStep;

    /** Constructor for domain-level requests where we are not master */
    private OperationRouting() {
        twoStep = false;
        singleHost = null;
    }

    /** Constructor for multi-host ops */
    private OperationRouting(final boolean twoStep) {
        this.twoStep = twoStep;
        singleHost = null;
    }

    /**
     * Constructor for a request routed to a single host
     *
     * @param host the name of the host
     * @param twoStep true if a two-step execution is needed
     */
    public OperationRouting(String host, boolean twoStep) {
        this.hosts.add(host);
        this.twoStep = twoStep;
        singleHost = host;
        hosts.add(host);
    }

    /**
     * Constructor for a request routed to multiple hosts
     *
     * @param hosts the names of the hosts
     */
    public OperationRouting(final Collection<String> hosts) {
        this.hosts.addAll(hosts);
        this.twoStep = true;
        singleHost = null;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public String getSingleHost() {
        return singleHost;
    }

    public boolean isTwoStep() {
        return twoStep;
    }

    public boolean isLocalOnly(final String localHostName) {
        if (singleHost != null) {
            return localHostName.equals(singleHost);
        } else {
            return hosts.size() == 1 && hosts.contains(localHostName);
        }
    }

    public boolean isLocalCallNeeded(final String localHostName) {
        return localHostName.equals(singleHost) || hosts.size() == 0 || hosts.contains(localHostName);
    }
}

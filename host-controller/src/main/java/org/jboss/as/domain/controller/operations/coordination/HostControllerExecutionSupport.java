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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.dmr.ModelNode;

/**
* Support class for the execution of an operation on an individual host controller.
*
* @author Brian Stansberry (c) 2011 Red Hat Inc.
*/
interface HostControllerExecutionSupport {

    /**
     * Gets the operation (if any) that should be run on the host controller itself
     * @return the operation to run on the host controller, or {@code null}
     */
    ModelNode getDomainOperation();

    /**
     * Gets the operations that should be run on the servers managed by the host controller.
     *
     * @param provider provider of server operations this object can delegate to if needed
     *
     * @return map of servers to the operation they should execute. Will not be {@code null} but may be empty
     */
    Map<Set<ServerIdentity>, ModelNode> getServerOps(ServerOperationProvider provider);

    /**
     * Gets the result of this operation (if any) on this host controller, along with any operations
     * needed to effect the operation on the servers managed by this host controller, in the
     * format expected by the host controller that is coordinating overall execution across the domain.
     *
     * @param resultNode node to which the result should be attached
     *
     * @return the formatted result
     */
    ModelNode getFormattedDomainResult(ModelNode resultNode);

    /**
     * Provider of server level operations necessary to effect a given domain or host level operation on the servers
     * managed by this host controller.
     */
    interface ServerOperationProvider {

        /**
         * Gets the server level operations necessary to effect a given domain or host level operation on the servers.
         *
         * @param domainOp the domain or host level operation
         * @param address the address of the domain level operation
         *
         * @return map of servers to the operation they should execute. Will not be {@code null} but may be empty
         */
        Map<Set<ServerIdentity>, ModelNode> getServerOperations(ModelNode domainOp, PathAddress address);
    }

    /** Provides a reference to a ModelNode representation of the domain model to {@link Factory} */
    interface DomainModelProvider {
        /**
         * Gets a ModelNode representation of the domain model
         * @return the model. Cannot be {@code null}
         */
        ModelNode getDomainModel();
    }

    /** Provides a factory method for creating {@link HostControllerExecutionSupport} instances */
    class Factory {

        /**
         * Create a HostControllerExecutionSupport for a given operation.
         *
         * @param operation the operation
         * @param hostName the name of the host executing the operation
         * @param domainModelProvider source for the domain model
         * @param ignoredDomainResourceRegistry registry of resource addresses that should be ignored
         *
         * @return the HostControllerExecutionSupport
         */
        public static HostControllerExecutionSupport create(final ModelNode operation,
                                                            final String hostName,
                                                            final DomainModelProvider domainModelProvider,
                                                            final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry) {
            return create(operation, hostName, domainModelProvider, ignoredDomainResourceRegistry, null);
        }

        private static HostControllerExecutionSupport create(final ModelNode operation,
                                                            final String hostName,
                                                            final DomainModelProvider domainModelProvider,
                                                            final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                            final Integer index) {
            String targetHost = null;
            String runningServerTarget = null;
            ModelNode runningServerOp = null;

            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            if (address.size() > 0) {
                PathElement first = address.getElement(0);
                if (HOST.equals(first.getKey())) {
                    targetHost = first.getValue();
                    if (address.size() > 1 && RUNNING_SERVER.equals(address.getElement(1).getKey())) {
                        runningServerTarget = address.getElement(1).getValue();
                        ModelNode relativeAddress = new ModelNode();
                        for (int i = 2; i < address.size(); i++) {
                            PathElement element = address.getElement(i);
                            relativeAddress.add(element.getKey(), element.getValue());
                        }
                        runningServerOp = operation.clone();
                        runningServerOp.get(OP_ADDR).set(relativeAddress);
                    }
                }
            }

            HostControllerExecutionSupport result;


            if (targetHost != null && !hostName.equals(targetHost)) {
                // ParsedOp representing another host
                result = new IgnoredOpExecutionSupport(index);
            }
            else if (runningServerTarget != null) {
                // ParsedOp representing a server op
                final ModelNode domainModel = domainModelProvider.getDomainModel();
                final String serverGroup = domainModel.require(HOST).require(targetHost).require(SERVER_CONFIG).require(runningServerTarget).require(GROUP).asString();
                final ServerIdentity serverIdentity = new ServerIdentity(targetHost, serverGroup, runningServerTarget);
                result = new DirectServerOpExecutionSupport(index, serverIdentity, runningServerOp);
            }
            else if (COMPOSITE.equals(operation.require(OP).asString())) {
                // Recurse into the steps to see what's required
                if (operation.hasDefined(STEPS)) {
                    int stepNum = 0;
                    List<HostControllerExecutionSupport> parsedSteps = new ArrayList<HostControllerExecutionSupport>();
                    for (ModelNode step : operation.get(STEPS).asList()) {
                        parsedSteps.add(create(step, hostName, domainModelProvider, ignoredDomainResourceRegistry, stepNum++));
                    }
                    result = new MultiStepOpExecutionSupport(index, parsedSteps);
                }
                else {
                    // Will fail later
                    result = new DomainOpExecutionSupport(index, operation, address);
                }
            }
            else if (targetHost == null && ignoredDomainResourceRegistry.isResourceExcluded(address)) {
                result = new IgnoredOpExecutionSupport(index);
            }
            else {
                // ParsedOp for this host
                result = new DomainOpExecutionSupport(index, operation, address);
            }

            return result;

        }

        private static class IgnoredOpExecutionSupport extends SimpleOpExecutionSupport {

            private IgnoredOpExecutionSupport(Integer index) {
                super(index);
            }

            @Override
            public ModelNode getDomainOperation() {
                return null;
            }

            @Override
            public Map<Set<ServerIdentity>, ModelNode> getServerOps(ServerOperationProvider provider) {
                return Collections.emptyMap();
            }
        }

        private static class DirectServerOpExecutionSupport extends SimpleOpExecutionSupport {
            private Map<Set<ServerIdentity>, ModelNode> serverOps;

            private DirectServerOpExecutionSupport(final Integer index, final ServerIdentity serverIdentity, ModelNode serverOp) {
                super(index);
                this.serverOps = Collections.singletonMap(Collections.singleton(serverIdentity), serverOp);
            }

            @Override
            public ModelNode getDomainOperation() {
                return null;
            }

            @Override
            public Map<Set<ServerIdentity>, ModelNode> getServerOps(ServerOperationProvider provider) {
                return serverOps;
            }
        }

        private static class DomainOpExecutionSupport extends SimpleOpExecutionSupport {

            private final ModelNode domainOp;
            private final PathAddress domainOpAddress;

            private DomainOpExecutionSupport(Integer index, ModelNode domainOp, final PathAddress domainOpAddress) {
                super(index);
                this.domainOp = domainOp;
                this.domainOpAddress = domainOpAddress;
            }

            @Override
            public ModelNode getDomainOperation() {
                return domainOp;
            }

            @Override
            public Map<Set<ServerIdentity>, ModelNode> getServerOps(ServerOperationProvider provider) {
                return provider.getServerOperations(domainOp, domainOpAddress);
            }
        }

        private abstract static class SimpleOpExecutionSupport implements HostControllerExecutionSupport {
            private final String domainStep;

            private SimpleOpExecutionSupport(Integer index) {
                this.domainStep = index == null ? null : "step-" + (index + 1);
            }

            @Override
            public ModelNode getFormattedDomainResult(ModelNode resultNode) {
                ModelNode formatted = new ModelNode();
                if (domainStep != null) {
                    formatted.get(domainStep).set(resultNode);
                } else {
                    formatted.set(resultNode);
                }
                return formatted;
            }
        }

        private static class MultiStepOpExecutionSupport implements HostControllerExecutionSupport {

            private final String domainStep;
            private final List<HostControllerExecutionSupport> steps;

            private MultiStepOpExecutionSupport(final Integer index, final List<HostControllerExecutionSupport> steps) {
                this.steps = steps;
                this.domainStep = index == null ? null : "step-" + (index + 1);
            }

            @Override
            public Map<Set<ServerIdentity>, ModelNode> getServerOps(ServerOperationProvider provider) {
                Map<Set<ServerIdentity>, List<ModelNode>> buildingBlocks = new HashMap<Set<ServerIdentity>, List<ModelNode>>();
                for (HostControllerExecutionSupport step : steps) {
                    Map<Set<ServerIdentity>, ModelNode> stepResult = step.getServerOps(provider);
                    if (stepResult.size() == 0) {
                        continue;
                    }
                    else if (buildingBlocks.size() == 0) {
                        for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : stepResult.entrySet()) {
                            List<ModelNode> list = new ArrayList<ModelNode>();
                            list.add(entry.getValue());
                            buildingBlocks.put(entry.getKey(), list);
                        }
                    }
                    else {
                        for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : stepResult.entrySet()) {
                            List<ModelNode> existingOp = buildingBlocks.get(entry.getKey());
                            if (existingOp != null) {
                                existingOp.add(entry.getValue());
                            }
                            else {
                                Set<ServerIdentity> newSet = new HashSet<ServerIdentity>(entry.getKey());
                                Set<Set<ServerIdentity>> existingSets = new HashSet<Set<ServerIdentity>>(buildingBlocks.keySet());
                                for (Set<ServerIdentity> existing : existingSets) {
                                    Set<ServerIdentity> copy = new HashSet<ServerIdentity>(existing);
                                    copy.retainAll(newSet);
                                    if (copy.size() > 0) {
                                        if (copy.size() == existing.size()) {
                                            // Just add the new step and store back
                                            buildingBlocks.get(existing).add(entry.getValue());
                                        }
                                        else {
                                            // Add the new step to the intersection; store the old set of steps
                                            // under a key that includes the remainder
                                            List<ModelNode> existingSteps = buildingBlocks.remove(existing);
                                            List<ModelNode> newSteps = new ArrayList<ModelNode>(existingSteps);
                                            buildingBlocks.put(copy, newSteps);
                                            existing.removeAll(copy);
                                            buildingBlocks.put(existing, existingSteps);
                                        }

                                        // no longer track the servers we've stored
                                        newSet.removeAll(copy);
                                    }
                                }

                                // Any servers not stored above get their own entry
                                if (newSet.size() > 0) {
                                    List<ModelNode> toAdd = new ArrayList<ModelNode>();
                                    toAdd.add(entry.getValue());
                                    buildingBlocks.put(newSet, toAdd);
                                }
                            }
                        }
                    }
                }

                Map<Set<ServerIdentity>, ModelNode> result;
                if (buildingBlocks.size() > 0) {
                    result = new HashMap<Set<ServerIdentity>, ModelNode>();
                    for (Map.Entry<Set<ServerIdentity>, List<ModelNode>> entry : buildingBlocks.entrySet()) {
                        List<ModelNode> ops = entry.getValue();
                        if (ops.size() == 1) {
                            result.put(entry.getKey(), ops.get(0));
                        }
                        else {
                            ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
                            ModelNode steps = composite.get(STEPS);
                            for (ModelNode step : entry.getValue()) {
                                steps.add(step);
                            }
                            result.put(entry.getKey(), composite);
                        }
                    }
                }
                else {
                    result = Collections.emptyMap();
                }
                return result;
            }

            @Override
            public ModelNode getDomainOperation() {

                List<ModelNode> domainSteps = new ArrayList<ModelNode>();
                for (HostControllerExecutionSupport step : steps) {
                    ModelNode stepNode = step.getDomainOperation();
                    if (stepNode != null) {
                        domainSteps.add(stepNode);
                    }
                }
                if (domainSteps.size() == 0) {
                    //Nothing to do, return null
                    return null;
                }
                //
                ModelNode stepsParam = new ModelNode();
                for (ModelNode stepNode : domainSteps) {
                    stepsParam.add(stepNode);
                }

                ModelNode result = Util.getEmptyOperation(COMPOSITE, new ModelNode());
                result.get(STEPS).set(stepsParam);
                return result;
            }

            @Override
            public ModelNode getFormattedDomainResult(ModelNode resultNode) {

                ModelNode allSteps = new ModelNode();
                int resultStep = 0;
                for (int i = 0; i < steps.size(); i++) {
                    HostControllerExecutionSupport po = steps.get(i);
                    if (po.getDomainOperation() != null) {
                        String label = "step-" + (++resultStep);
                        ModelNode stepResultNode = resultNode.get(label);
                        ModelNode formattedStepResultNode = po.getFormattedDomainResult(stepResultNode);
                        allSteps.get("step-" + (i + 1)).set(formattedStepResultNode);
                    }
                    else {
                        allSteps.get("step-" + (i + 1), OUTCOME).set(IGNORED);
                    }
                }
                ModelNode formatted = new ModelNode();
                if (domainStep != null) {
                    formatted.get(domainStep).set(allSteps);
                } else {
                    formatted.set(allSteps);
                }
                return formatted;
            }
        }
    }
}

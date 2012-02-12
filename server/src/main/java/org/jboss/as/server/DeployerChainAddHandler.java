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

package org.jboss.as.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import org.jboss.as.server.deployment.DeployerChainsService;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.dmr.ModelNode;

/**
 * @author John Bailey
 */
public class DeployerChainAddHandler implements OperationStepHandler, DescriptionProvider {
    static final String NAME = "add-deployer-chains";
    public static final DeployerChainAddHandler INSTANCE = new DeployerChainAddHandler();

    static void addDeploymentProcessor(Phase phase, int priority, DeploymentUnitProcessor processor) {
        final EnumMap<Phase, Set<RegisteredProcessor>> deployerMap = INSTANCE.deployerMap;
        deployerMap.get(phase).add(new RegisteredProcessor(priority, processor));
    }

    static ModelNode OPERATION = new ModelNode();
    static {
        OPERATION.get(ModelDescriptionConstants.OP).set(NAME);
        OPERATION.get(ADDRESS).setEmptyList();
    }

    // This map is concurrently read by multiple threads but will only
    // be written by a single thread, the boot thread
    private final EnumMap<Phase, Set<RegisteredProcessor>> deployerMap;

    private DeployerChainAddHandler() {
        final EnumMap<Phase, Set<RegisteredProcessor>> map = new EnumMap<Phase, Set<RegisteredProcessor>>(Phase.class);
        for (Phase phase : Phase.values()) {
            map.put(phase, new ConcurrentSkipListSet<RegisteredProcessor>());
        }
        this.deployerMap = map;
    }

    /** This is only public so AbstractSubsystemTest can use it; otherwise it would be package-protected. */
    public void clearDeployerMap() {
        for (Set<RegisteredProcessor> set : deployerMap.values()) {
            set.clear();
        }
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(context.getType() == OperationContext.Type.SERVER) {

            // Our real work needs to run after all RUNTIME steps that add DUPs have run. ServerService adds
            // this operation at the end of the boot op list, so our MODEL stage step is last, and
            // this RUNTIME step we are about to add should therefore be last as well *at the time
            // we register it*. However, other RUNTIME steps can themselves add new RUNTIME steps that
            // will then come after this one. So we do the same -- add a RUNTIME step that adds another
            // RUNTIME step that does the real work.
            // Theoretically this kind of "predecessor runtime step adds another runtime step, so we have to
            // add one to come later" business could go on forever. But any operation that does that with
            // a DUP-add step is just broken and should just find another way.

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    context.addStep(new FinalRuntimeStepHandler(), OperationContext.Stage.RUNTIME);

                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        //Since this instance should have EntryType.PRIVATE, there is no need for a description
        return new ModelNode();
    }

    private class FinalRuntimeStepHandler implements OperationStepHandler {

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            if (deployerMap == null) {
                throw new IllegalStateException("No deployers set");
            }
            final EnumMap<Phase, List<DeploymentUnitProcessor>> finalDeployers = new EnumMap<Phase, List<DeploymentUnitProcessor>>(Phase.class);
            final List<DeploymentUnitProcessor> processorList = new ArrayList<DeploymentUnitProcessor>(256);
            for (Phase phase : Phase.values()) {
                processorList.clear();
                final Set<RegisteredProcessor> processorSet = deployerMap.get(phase);
                for (RegisteredProcessor processor : processorSet) {
                    processorList.add(processor.getProcessor());
                }
                finalDeployers.put(phase, Arrays.asList(processorList.toArray(new DeploymentUnitProcessor[processorList.size()])));
            }
            final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
            DeployerChainsService.addService(context.getServiceTarget(), finalDeployers, verificationHandler);

            context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    context.removeService(Services.JBOSS_DEPLOYMENT_CHAINS);
                }
            });
        }
    }

    static final class RegisteredProcessor implements Comparable<RegisteredProcessor> {
        private final int priority;
        private final DeploymentUnitProcessor processor;

        RegisteredProcessor(final int priority, final DeploymentUnitProcessor processor) {
            this.priority = priority;
            this.processor = processor;
        }

        @Override
        public int compareTo(final RegisteredProcessor o) {
            final int rel = Integer.signum(priority - o.priority);
            return rel == 0 ? processor.getClass().getName().compareTo(o.getClass().getName()) : rel;
        }

        int getPriority() {
            return priority;
        }

        DeploymentUnitProcessor getProcessor() {
            return processor;
        }
    }
}

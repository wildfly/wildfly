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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.ProfileDescription;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Outputs the profile as a series of operations needed to construct the profile
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProfileDescribeHandler implements OperationStepHandler, DescriptionProvider {

    public static final ProfileDescribeHandler INSTANCE = new ProfileDescribeHandler();

    private ProfileDescribeHandler() {
    }


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String opName = operation.require(OP).asString();
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));

        final ModelNode result = new ModelNode();
        final ModelNode profile = context.readModel(PathAddress.EMPTY_ADDRESS);
        result.setEmptyList();

        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        final AtomicReference<ModelNode> failureRef = new AtomicReference<ModelNode>();

        final ModelNode subsystemResults = new ModelNode().setEmptyList();
        final Map<String, ModelNode> includeResults = new HashMap<String, ModelNode>();

        // Add a step at end to assemble all the data
        // Add steps in the reverse of expected order, as Stage.IMMEDIATE adds to the top of the list
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                boolean failed = false;
                if (failureRef.get() != null) {
                    // One of our subsystems failed
                    context.getFailureDescription().set(failureRef.get());
                    failed = true;
                } else {
                    for (ModelNode includeRsp : includeResults.values()) {
                        if (includeRsp.hasDefined(FAILURE_DESCRIPTION)) {
                            context.getFailureDescription().set(includeRsp.get(FAILURE_DESCRIPTION));
                            failed = true;
                            break;
                        }
                        ModelNode includeResult = includeRsp.get(RESULT);
                        if (includeResult.isDefined()) {
                            for (ModelNode op : includeResult.asList()) {
                                result.add(op);
                            }
                        }
                    }
                }
                if (!failed) {
                    for (ModelNode subsysRsp : subsystemResults.asList()) {
                        result.add(subsysRsp);
                    }
                    context.getResult().set(result);
                }
                context.completeStep();
            }
        }, OperationContext.Stage.IMMEDIATE);

        if (profile.hasDefined(SUBSYSTEM)) {
            for (final String subsystemName : profile.get(SUBSYSTEM).keys()) {
                final ModelNode subsystemRsp = new ModelNode();
                PathElement pe = PathElement.pathElement(SUBSYSTEM, subsystemName);
                PathAddress fullAddress = address.append(pe);
                final ModelNode subsystemAddress = fullAddress.toModelNode();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(subsystemAddress);
                PathAddress relativeAddress = PathAddress.pathAddress(pe);
                OperationStepHandler subsysHandler = registry.getOperationHandler(relativeAddress, opName);
                if (subsysHandler == null) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.noHandlerForOperation(opName, fullAddress)));
                }

                // Step to store subsystem ops in overall list
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        if (failureRef.get() == null) {
                            if (subsystemRsp.hasDefined(FAILURE_DESCRIPTION)) {
                                failureRef.set(subsystemRsp.get(FAILURE_DESCRIPTION));
                            } else if (subsystemRsp.hasDefined(RESULT)) {
                                for (ModelNode op : subsystemRsp.require(RESULT).asList()) {
                                    subsystemResults.add(op);
                                }
                            }
                        }
                        context.completeStep();
                    }
                }, OperationContext.Stage.IMMEDIATE);

                // Step to determine subsystem ops
                context.addStep(subsystemRsp, newOp, subsysHandler, OperationContext.Stage.IMMEDIATE);
            }
        }

        if (profile.hasDefined(INCLUDES)) {
            // Call this op for each included profile
            for (ModelNode include : profile.get(INCLUDES).asList()) {

                final String includeName = include.asString();
                final ModelNode includeRsp = new ModelNode();
                includeResults.put(includeName, includeRsp);

                final ModelNode includeAddress = address.subAddress(0, address.size() - 1).append(PathElement.pathElement(PROFILE, includeName)).toModelNode();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(includeAddress);

                context.addStep(includeRsp, newOp, INSTANCE, OperationContext.Stage.IMMEDIATE);
            }
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ProfileDescription.getProfileDescribeOperation(locale);
    }
}

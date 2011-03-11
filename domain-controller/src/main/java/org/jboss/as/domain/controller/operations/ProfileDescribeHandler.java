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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * Outputs the profile as a series of operations needed to construct the profile
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProfileDescribeHandler implements ModelQueryOperationHandler {

    public static final ProfileDescribeHandler INSTANCE = new ProfileDescribeHandler();

    private ProfileDescribeHandler() {

    }


    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));

        final ModelNode result = new ModelNode();
        final ModelNode profile = context.getSubModel();
        result.setEmptyList();

        if (profile.hasDefined(INCLUDES)) {

            for (ModelNode include : profile.get(INCLUDES).asList()) {
                final ModelNode includeAddress = address.subAddress(0, address.size() - 1).append(PathElement.pathElement(PROFILE, include.asString())).toModelNode();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(includeAddress);
                final ModelNode newOpResult = context.getController().execute(OperationBuilder.Factory.create(newOp).build());
                if (FAILED.equals(newOpResult.get(OUTCOME).asString())) {
                    throw new OperationFailedException(newOpResult.get(FAILURE_DESCRIPTION));
                }
                for (ModelNode op : newOpResult.require(RESULT).asList()) {
                    result.add(op);
                }
            }
        }

        if (profile.hasDefined(SUBSYSTEM)) {
            for (String subsystemName : profile.get(SUBSYSTEM).keys()) {
                final ModelNode subsystemAddress = address.append(PathElement.pathElement(SUBSYSTEM, subsystemName)).toModelNode();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(subsystemAddress);
                final ModelNode newOpResult = context.getController().execute(OperationBuilder.Factory.create(newOp).build());
                if (FAILED.equals(newOpResult.get(OUTCOME).asString())) {
                    throw new OperationFailedException(newOpResult.get(FAILURE_DESCRIPTION));
                }
                for (ModelNode op : newOpResult.require(RESULT).asList()) {
                    result.add(op);
                }
            }
        }

        resultHandler.handleResultFragment(Util.NO_LOCATION, result);
        resultHandler.handleResultComplete();
        return new BasicOperationResult();
    }
}

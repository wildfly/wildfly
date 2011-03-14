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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.dmr.ModelNode;

/**
 * Workaround handler to get host proxy names.
 */
public class ReadChildrenNamesHandler extends org.jboss.as.controller.operations.global.GlobalOperationHandlers.ReadChildrenOperationHandler {

    private final DomainModelImpl domainModelImpl;
    public ReadChildrenNamesHandler(final DomainModelImpl domainModelImpl) {
        this.domainModelImpl = domainModelImpl;
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        if (address.size() == 0 && domainModelImpl.isMaster()) {
            final String childName = operation.require(CHILD_TYPE).asString();
            if(HOST.equals(childName)) {
                final ModelNode result = new ModelNode().setEmptyList();
                for(final String hostName : domainModelImpl.getHostNames()) {
                    result.add(hostName);
                }
                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                resultHandler.handleResultComplete();
                return new BasicOperationResult();
            }
        }
        return super.execute(context, operation, resultHandler);
    }

}

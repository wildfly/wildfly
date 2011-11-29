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

import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.common.PathDescription.RELATIVE_TO;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding a path.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SpecifiedPathAddHandler extends PathAddHandler {

    public static SpecifiedPathAddHandler INSTANCE = new SpecifiedPathAddHandler();

    private SpecifiedPathAddHandler() {
        super(true);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String name = address.getLastElement().getValue();
        ModelNode pathNode = operation.get(PATH);
        ModelNode relNode = operation.get(RELATIVE_TO);
        String path = pathNode.isDefined() ? pathNode.asString() : null;
        String relativeTo = relNode.isDefined() ? relNode.asString() : null;

        final ServiceTarget target = context.getServiceTarget();
        if (relativeTo == null) {
            newControllers.add(AbsolutePathService.addService(name, path, target, newControllers, verificationHandler));
        } else {
            newControllers.add(RelativePathService.addService(name, path, relativeTo, target, newControllers, verificationHandler));
        }
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}

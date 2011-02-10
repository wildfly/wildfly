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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.host.controller.descriptions.HostServerDescription;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class LocalDomainControllerAddHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "write-local-domain-controller";

    public static ModelNode getAddDomainControllerOperation(ModelNode address) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        return op;
    }

    public static final LocalDomainControllerAddHandler INSTANCE = new LocalDomainControllerAddHandler();

    /**
     * Create the ServerAddHandler
     */
    LocalDomainControllerAddHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        try {
            final ModelNode model = context.getSubModel();
            model.get(DOMAIN_CONTROLLER).get(LOCAL).setEmptyObject();
            ModelNode compensating = Util.getResourceRemoveOperation(operation.get(OP_ADDR));
            resultHandler.handleResultComplete(compensating);
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return Cancellable.NULL;
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostServerDescription.getServerAddOperation(locale);
    }
}

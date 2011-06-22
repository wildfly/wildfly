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
package org.jboss.as.server.services.net;

import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;

import java.util.Locale;

import org.jboss.as.controller.descriptions.common.SocketBindingGroupDescription;
import org.jboss.as.controller.operations.common.AbstractSocketBindingGroupAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for the domain socket-binding-group resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BindingGroupAddHandler extends AbstractSocketBindingGroupAddHandler {

    private static final ParametersValidator VALIDATOR = new ParametersValidator();

    static {
        VALIDATOR.registerValidator(DEFAULT_INTERFACE, new StringLengthValidator(1, Integer.MAX_VALUE, false, true));
        VALIDATOR.registerValidator(PORT_OFFSET, new IntRangeValidator(0, 65535, true, true));
    }

    public static final ModelNode getOperation(ModelNode address, ModelNode model) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        op.get(DEFAULT_INTERFACE).set(model.get(DEFAULT_INTERFACE));
        op.get(PORT_OFFSET).set(model.get(PORT_OFFSET));
        return op;
    }

    public static final BindingGroupAddHandler INSTANCE = new BindingGroupAddHandler();

    private final ParametersValidator runtimeValidator = new ParametersValidator();

    private BindingGroupAddHandler() {
        super(VALIDATOR);
        runtimeValidator.registerValidator(DEFAULT_INTERFACE, new StringLengthValidator(1, Integer.MAX_VALUE, false, false));
        runtimeValidator.registerValidator(PORT_OFFSET, new IntRangeValidator(0, 65535, true, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SocketBindingGroupDescription.getServerSocketBindingGroupAddOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);
        ModelNode offset = operation.get(PORT_OFFSET);
        if (!offset.isDefined()) {
            offset.set(0);
        }
        model.get(PORT_OFFSET).set(offset);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // Resolve any expressions and re-validate
        final ModelNode resolvedOp = operation.resolve();
        runtimeValidator.validate(resolvedOp);
        int portOffset = resolvedOp.get(PORT_OFFSET).isDefined() ? resolvedOp.get(PORT_OFFSET).asInt() : 0;
        String defaultInterface = resolvedOp.require(DEFAULT_INTERFACE).asString();

        SocketBindingManagerService service = new SocketBindingManagerService(portOffset);
        final ServiceTarget serviceTarget = context.getServiceTarget();
        newControllers.add(serviceTarget.addService(SocketBindingManager.SOCKET_BINDING_MANAGER, service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(defaultInterface), NetworkInterfaceBinding.class, service.getDefaultInterfaceBinding())
                .addListener(verificationHandler)
                .install());

    }
}

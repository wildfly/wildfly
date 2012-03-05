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


import java.util.Locale;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;
import org.jboss.as.host.controller.descriptions.HostServerDescription;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} removing an existing server configuration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
public class ServerRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static final ServerRemoveHandler INSTANCE = new ServerRemoveHandler();

    /**
     * Create the InterfaceRemoveHandler
     */
    protected ServerRemoveHandler() {
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRemove(context, operation, model);
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String serverName = address.getLastElement().getValue();
        final ModelNode verifyOp = new ModelNode();
        verifyOp.get(OP).set("verify-running-server");
        verifyOp.get(OP_ADDR).add(HOST, address.getElement(0).getValue());
        context.addStep(context.getResult(), verifyOp, new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final PathAddress serverAddress = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(SERVER, serverName));
                final ProxyController controller = context.getResourceRegistration().getProxyController(serverAddress);
                if(! context.getResourceRegistration().getChildNames(PathAddress.EMPTY_ADDRESS).contains(SERVER)) {
                    throw new OperationFailedException(new ModelNode().set(context.getResourceRegistration().getChildNames(PathAddress.EMPTY_ADDRESS).toString()));
                }
                if(controller != null) {
                    context.getFailureDescription().set(MESSAGES.serverStillRunning(serverName));
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostServerDescription.getServerRemoveOperation(locale);
    }
}

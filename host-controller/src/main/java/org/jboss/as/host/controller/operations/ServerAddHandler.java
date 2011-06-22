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
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.host.controller.descriptions.HostServerDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@code OperationHandler} adding a new server configuration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddServerOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        op.get(GROUP).set(existing.get(GROUP));
        op.get(AUTO_START).set(existing.get(AUTO_START));
        if (existing.hasDefined(SOCKET_BINDING_GROUP)) {
            op.get(SOCKET_BINDING_GROUP).set(existing.get(SOCKET_BINDING_GROUP));
        }
        if (existing.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            op.get(SOCKET_BINDING_PORT_OFFSET).set(existing.get(SOCKET_BINDING_PORT_OFFSET));
        }

        return op;
    }

    public static final ServerAddHandler INSTANCE = new ServerAddHandler();

    private final ParametersValidator validator = new ParametersValidator();

    /**
     * Create the ServerAddHandler
     */
    private ServerAddHandler() {
        validator.registerValidator(GROUP, new StringLengthValidator(1, Integer.MAX_VALUE, false, true));
        validator.registerValidator(SOCKET_BINDING_GROUP, new StringLengthValidator(1, Integer.MAX_VALUE, true, true));
        validator.registerValidator(SOCKET_BINDING_PORT_OFFSET, new IntRangeValidator(0, 65535, true, true));
        validator.registerValidator(AUTO_START, new ModelTypeValidator(ModelType.BOOLEAN, true, true));
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        validator.validate(operation);

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        createCoreModel(model);
        model.get(NAME).set(name);
        model.get(GROUP).set(operation.require(GROUP));
        if (operation.hasDefined(SOCKET_BINDING_GROUP)) {
            model.get(SOCKET_BINDING_GROUP).set(operation.get(SOCKET_BINDING_GROUP));
        }
        if (operation.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            model.get(SOCKET_BINDING_PORT_OFFSET).set(operation.get(SOCKET_BINDING_PORT_OFFSET));
        }
        ModelNode autoStart = operation.hasDefined(AUTO_START) ? operation.get(AUTO_START) : new ModelNode().set(true);
        model.get(AUTO_START).set(autoStart);
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    private void createCoreModel(ModelNode root) {
        root.get(PATH);
        root.get(SYSTEM_PROPERTY);
        root.get(INTERFACE);
        root.get(JVM);
        // JBAS-9123 - always init socket-binding-group and port-offset
        root.get(SOCKET_BINDING_GROUP);
        root.get(SOCKET_BINDING_PORT_OFFSET);
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostServerDescription.getServerAddOperation(locale);
    }
}

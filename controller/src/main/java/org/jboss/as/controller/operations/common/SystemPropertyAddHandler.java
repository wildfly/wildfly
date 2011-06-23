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
package org.jboss.as.controller.operations.common;

import java.util.List;
import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * Base class for domain/host and server system property add handlers.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static final SystemPropertyAddHandler INSTANCE_WITH_BOOTTIME = new SystemPropertyAddHandler(true);
    public static final SystemPropertyAddHandler INSTANCE_WITHOUT_BOOTTIME = new SystemPropertyAddHandler(false);

    public static ModelNode getOperation(ModelNode address, String value) {
        return getOperation(address, value, null);
    }

    public static ModelNode getOperation(ModelNode address, String value, Boolean boottime) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        if (value == null) {
            op.get(VALUE).set(new ModelNode());
        } else {
            op.get(VALUE).set(value);
        }
        if (boottime != null) {
            op.get(BOOT_TIME).set(boottime);
        }
        return op;
    }


    private final ParametersValidator validator = new ParametersValidator();
    private final boolean useBoottime;

    /**
     * Create the SystemPropertyAddHandler
     */
    private SystemPropertyAddHandler(boolean useBoottime) {
        this.useBoottime = useBoottime;
        validator.registerValidator(VALUE, new StringLengthValidator(0, true));
        if (useBoottime) {
            validator.registerValidator(BOOT_TIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        validator.validate(operation);

        final String value = operation.get(VALUE).isDefined() ? operation.get(VALUE).asString() : null;
        if (value == null) {
            model.get(VALUE).set(new ModelNode());
        } else {
            model.get(VALUE).set(value);
        }
        if (useBoottime) {
            boolean boottime = operation.get(BOOT_TIME).isDefined() ? operation.get(BOOT_TIME).asBoolean() : true;
            model.get(BOOT_TIME).set(boottime);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final String value = operation.get(VALUE).isDefined() ? operation.get(VALUE).asString() : null;
        SecurityActions.setSystemProperty(name, value);
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }

    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddSystemPropertyOperation(locale, useBoottime);
    }
}

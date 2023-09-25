/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class AllowedChannelOptionTypesValidator extends ModelTypeValidator implements AllowedValuesValidator {
    public static final AllowedChannelOptionTypesValidator INSTANCE = new AllowedChannelOptionTypesValidator();

    private final List<ModelNode> allowedChannelOptTypes;

    private AllowedChannelOptionTypesValidator() {
        super(ModelType.STRING, false);
        allowedChannelOptTypes = new ArrayList<ModelNode>();
        allowedChannelOptTypes.add(new ModelNode().set("remoting"));
        allowedChannelOptTypes.add(new ModelNode().set("xnio"));
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return allowedChannelOptTypes;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()
                && value.getType() != ModelType.EXPRESSION
                && !this.allowedChannelOptTypes.contains(value)) {
            throw EjbLogger.ROOT_LOGGER.unknownChannelCreationOptionType(value.asString());
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.ws.common.Messages;
import org.jboss.ws.common.utils.AddressUtils;

/**
 * Validates addresses using the JBossWS common address utilities
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public class AddressValidator extends ModelTypeValidator {

    public AddressValidator(final boolean nullable, final boolean allowExpressions) {
        super(ModelType.STRING, nullable, allowExpressions, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            String address = value.asString();
            if (address.startsWith("[") && address.endsWith("]")) {
                address = address.substring(1, address.length() - 1);
            }
            if (!AddressUtils.isValidAddress(address)) {
                throw new OperationFailedException(Messages.MESSAGES.invalidAddressProvided(address));
            }
        }
    }

}


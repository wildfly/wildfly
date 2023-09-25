/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import io.undertow.predicate.PredicateParser;

/**
 * @author Stuart Douglas
 */
public class PredicateValidator extends ModelTypeValidator {

    public static final PredicateValidator INSTANCE = new PredicateValidator();

    private PredicateValidator() {
        super(ModelType.STRING, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            String val = value.asString();
            try {
                PredicateParser.parse(val, getClass().getClassLoader());
            } catch (Exception e) {
                throw new OperationFailedException(UndertowLogger.ROOT_LOGGER.predicateNotValid(val, e.getMessage()), e);
            }
        }
    }
}

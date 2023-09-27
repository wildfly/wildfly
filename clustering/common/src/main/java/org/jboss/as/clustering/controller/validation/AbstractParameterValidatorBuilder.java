/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.validation;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractParameterValidatorBuilder implements ParameterValidatorBuilder {

    boolean allowsUndefined = false;
    boolean allowsExpressions = false;

    @Override
    public ParameterValidatorBuilder configure(AttributeDefinition definition) {
        this.allowsExpressions = definition.isAllowExpression();
        this.allowsUndefined = !definition.isRequired();
        return this;
    }

    @Override
    public ParameterValidatorBuilder configure(AbstractAttributeDefinitionBuilder<?, ?> builder) {
        this.allowsExpressions = builder.isAllowExpression();
        this.allowsUndefined = builder.isNillable();
        return this;
    }
}

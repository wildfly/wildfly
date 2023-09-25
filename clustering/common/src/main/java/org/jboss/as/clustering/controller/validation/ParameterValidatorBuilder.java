/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.validation;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.operations.validation.ParameterValidator;

/**
 * Builder for a {@link ParameterValidator}.
 * @author Paul Ferraro
 */
public interface ParameterValidatorBuilder {

    /**
     * Configures this validator builder using the configuration of the specified attribute definition
     * @param definition an attribute definition
     * @return a reference to this builder
     */
    ParameterValidatorBuilder configure(AttributeDefinition definition);

    /**
     * Configures this validator builder using the configuration of the specified attribute definition builder
     * @param builder an attribute definition builder
     * @return a reference to this builder
     */
    ParameterValidatorBuilder configure(AbstractAttributeDefinitionBuilder<?, ?> builder);

    /**
     * Builds the validator.
     * @return a parameter validator
     */
    ParameterValidator build();
}

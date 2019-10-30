/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.dmr;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
interface Attributes {
    SimpleAttributeDefinition WSDL_HOST = new SimpleAttributeDefinitionBuilder(Constants.WSDL_HOST, ModelType.STRING)
            .setRequired(false)
            .setMinSize(1)
            .setAllowExpression(true)
            .setValidator(new AddressValidator(true, true))
            .build();

    SimpleAttributeDefinition WSDL_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_PORT, ModelType.INT)
            .setRequired(false)
            .setMinSize(1)
            .setValidator(new IntRangeValidator(1, true, true))
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition WSDL_SECURE_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_SECURE_PORT, ModelType.INT)
            .setRequired(false)
            .setMinSize(1)
            .setValidator(new IntRangeValidator(1, true, true))
            .setAllowExpression(true)
            .build();
    SimpleAttributeDefinition WSDL_URI_SCHEME = new SimpleAttributeDefinitionBuilder(Constants.WSDL_URI_SCHEME, ModelType.STRING)
            .setRequired(false)
            .setMinSize(1)
            .setValidator(EnumValidator.create(WsdlUriSchema.class))
            .setAllowExpression(true)
            .build();
    enum WsdlUriSchema {http, https}

    SimpleAttributeDefinition MODIFY_WSDL_ADDRESS = new SimpleAttributeDefinitionBuilder(Constants.MODIFY_WSDL_ADDRESS, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition WSDL_PATH_REWRITE_RULE = new SimpleAttributeDefinitionBuilder(Constants.WSDL_PATH_REWRITE_RULE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .build();

    SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(Constants.STATISTICS_ENABLED, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {MODIFY_WSDL_ADDRESS, WSDL_HOST, WSDL_PORT, WSDL_SECURE_PORT, WSDL_URI_SCHEME, WSDL_PATH_REWRITE_RULE};

    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(Constants.CLASS, ModelType.STRING)
            .setRequired(true)
            .build();

    SimpleAttributeDefinition PROTOCOL_BINDINGS = new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL_BINDINGS, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
}

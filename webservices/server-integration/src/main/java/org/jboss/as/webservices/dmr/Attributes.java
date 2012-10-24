package org.jboss.as.webservices.dmr;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
interface Attributes {
    SimpleAttributeDefinition WSDL_HOST = new SimpleAttributeDefinitionBuilder(Constants.WSDL_HOST, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();

    SimpleAttributeDefinition WSDL_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_PORT, ModelType.INT)
            .setAllowNull(true)
            .setMinSize(1)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    SimpleAttributeDefinition WSDL_SECURE_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_SECURE_PORT, ModelType.INT)
            .setAllowNull(true)
            .setMinSize(1)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    SimpleAttributeDefinition MODIFY_WSDL_ADDRESS = new SimpleAttributeDefinitionBuilder(Constants.MODIFY_WSDL_ADDRESS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(true))
            .setMinSize(1)
            .setAllowExpression(true)
            .build();
    SimpleAttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {MODIFY_WSDL_ADDRESS, WSDL_HOST, WSDL_PORT, WSDL_SECURE_PORT};

    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(Constants.CLASS, ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition PROTOCOL_BINDINGS = new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL_BINDINGS, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();


}

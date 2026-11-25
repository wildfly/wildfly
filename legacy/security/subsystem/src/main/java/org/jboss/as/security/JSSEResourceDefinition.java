/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 * @author Tomaz Cerar
 */
class JSSEResourceDefinition extends SimpleResourceDefinition {

    static final ObjectTypeAttributeDefinition KEYSTORE = new ObjectTypeAttributeDefinition.Builder(Constants.KEYSTORE, ComplexAttributes.KEY_STORE_FIELDS)
            .setValidator(new ComplexAttributes.KeyStoreAttributeValidator(Constants.KEYSTORE)).setAttributeMarshaller(new ComplexAttributes.KeyStoreAttributeMarshaller()).build();

    static final ObjectTypeAttributeDefinition TRUSTSTORE = new ObjectTypeAttributeDefinition.Builder(Constants.TRUSTSTORE, ComplexAttributes.KEY_STORE_FIELDS)
            .setValidator(new ComplexAttributes.KeyStoreAttributeValidator(Constants.TRUSTSTORE)).setAttributeMarshaller(new ComplexAttributes.KeyStoreAttributeMarshaller()).build();

    static final ObjectTypeAttributeDefinition KEYMANAGER = new ObjectTypeAttributeDefinition.Builder(Constants.KEY_MANAGER, ComplexAttributes.KEY_MANAGER_FIELDS)
            .setAttributeMarshaller(new ComplexAttributes.KeyManagerAttributeMarshaller())
            .build();

    static final ObjectTypeAttributeDefinition TRUSTMANAGER = new ObjectTypeAttributeDefinition.Builder(Constants.TRUST_MANAGER, ComplexAttributes.KEY_MANAGER_FIELDS)
            .setAttributeMarshaller(new ComplexAttributes.KeyManagerAttributeMarshaller())
            .build();
    static final SimpleAttributeDefinition CLIENT_ALIAS = new SimpleAttributeDefinitionBuilder(Constants.CLIENT_ALIAS, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition SERVER_ALIAS = new SimpleAttributeDefinitionBuilder(Constants.SERVER_ALIAS, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition SERVICE_AUTH_TOKEN = new SimpleAttributeDefinitionBuilder(Constants.SERVICE_AUTH_TOKEN, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition CLIENT_AUTH = new SimpleAttributeDefinitionBuilder(Constants.CLIENT_AUTH, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition PROTOCOLS = new SimpleAttributeDefinitionBuilder(Constants.PROTOCOLS, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition CIPHER_SUITES = new SimpleAttributeDefinitionBuilder(Constants.CIPHER_SUITES, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    static final PropertiesAttributeDefinition ADDITIONAL_PROPERTIES = new PropertiesAttributeDefinition.Builder(Constants.ADDITIONAL_PROPERTIES, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshaller.PROPERTIES_MARSHALLER_UNWRAPPED)
            .setAttributeParser(AttributeParser.PROPERTIES_PARSER_UNWRAPPED)
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = {KEYSTORE, TRUSTSTORE, KEYMANAGER, TRUSTMANAGER, CLIENT_ALIAS, SERVER_ALIAS, SERVICE_AUTH_TOKEN,
        CLIENT_AUTH, PROTOCOLS, CIPHER_SUITES, ADDITIONAL_PROPERTIES};

    public static final JSSEResourceDefinition INSTANCE = new JSSEResourceDefinition();

    private JSSEResourceDefinition() {
        super(SecurityExtension.JSSE_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.JSSE),
                new ModelOnlyAddStepHandler(ATTRIBUTES),
                ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ModelOnlyWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

}

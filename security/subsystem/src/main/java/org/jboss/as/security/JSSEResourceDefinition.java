/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 * @author Tomaz Cerar
 */
public class JSSEResourceDefinition extends SimpleResourceDefinition {

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
                JSSEResourceDefinitionAdd.INSTANCE,
                new SecurityDomainReloadRemoveHandler());
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        SecurityDomainReloadWriteHandler writeHandler = new SecurityDomainReloadWriteHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    static class JSSEResourceDefinitionAdd extends SecurityDomainReloadAddHandler {

        static final JSSEResourceDefinitionAdd INSTANCE = new JSSEResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attr : ATTRIBUTES) {
                attr.validateAndSet(operation, model);
            }
        }
    }

}

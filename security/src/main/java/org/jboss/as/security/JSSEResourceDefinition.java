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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 */
public class JSSEResourceDefinition extends SimpleResourceDefinition {

    public static final JSSEResourceDefinition INSTANCE = new JSSEResourceDefinition();

    public static final KeyStoreAttributeDefinition KEYSTORE = new KeyStoreAttributeDefinition(Constants.KEYSTORE);
    public static final KeyStoreAttributeDefinition TRUSTSTORE = new KeyStoreAttributeDefinition(Constants.TRUSTSTORE);
    public static final KeyManagerAttributeDefinition KEYMANAGER = new KeyManagerAttributeDefinition(Constants.KEY_MANAGER);
    public static final KeyManagerAttributeDefinition TRUSTMANAGER = new KeyManagerAttributeDefinition(Constants.TRUST_MANAGER);
    public static final SimpleAttributeDefinition CLIENT_ALIAS =
            new SimpleAttributeDefinitionBuilder(Constants.CLIENT_ALIAS, ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition SERVER_ALIAS =
            new SimpleAttributeDefinitionBuilder(Constants.SERVER_ALIAS, ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition SERVICE_AUTH_TOKEN =
            new SimpleAttributeDefinitionBuilder(Constants.SERVICE_AUTH_TOKEN, ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition CLIENT_AUTH =
            new SimpleAttributeDefinitionBuilder(Constants.CLIENT_AUTH, ModelType.BOOLEAN, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition PROTOCOLS =
            new SimpleAttributeDefinitionBuilder(Constants.PROTOCOLS, ModelType.STRING, true).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition CIPHER_SUITES =
            new SimpleAttributeDefinitionBuilder(Constants.CIPHER_SUITES, ModelType.STRING, true).setAllowExpression(true).build();

    public static final PropertiesAttributeDefinition ADDITIONAL_PROPERTIES = new PropertiesAttributeDefinition(Constants.ADDITIONAL_PROPERTIES, Constants.PROPERTY, true);

    private JSSEResourceDefinition() {
        super(PathElement.pathElement(Constants.JSSE, Constants.CLASSIC),
              SecurityExtension.getResourceDescriptionResolver(Constants.JSSE),
              JSSEResourceDefinitionAdd.INSTANCE, new SecurityDomainReloadRemoveHandler());
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(KEYSTORE, null, new SecurityDomainReloadWriteHandler(KEYSTORE));
        resourceRegistration.registerReadWriteAttribute(TRUSTSTORE, null, new SecurityDomainReloadWriteHandler(TRUSTSTORE));
        resourceRegistration.registerReadWriteAttribute(KEYMANAGER, null, new SecurityDomainReloadWriteHandler(KEYMANAGER));
        resourceRegistration.registerReadWriteAttribute(TRUSTMANAGER, null, new SecurityDomainReloadWriteHandler(TRUSTMANAGER));
        resourceRegistration.registerReadWriteAttribute(CLIENT_ALIAS, null, new SecurityDomainReloadWriteHandler(CLIENT_ALIAS));
        resourceRegistration.registerReadWriteAttribute(SERVER_ALIAS, null, new SecurityDomainReloadWriteHandler(SERVER_ALIAS));
        resourceRegistration.registerReadWriteAttribute(SERVICE_AUTH_TOKEN, null, new SecurityDomainReloadWriteHandler(SERVICE_AUTH_TOKEN));
        resourceRegistration.registerReadWriteAttribute(CLIENT_AUTH, null, new SecurityDomainReloadWriteHandler(CLIENT_AUTH));
        resourceRegistration.registerReadWriteAttribute(PROTOCOLS, null, new SecurityDomainReloadWriteHandler(PROTOCOLS));
        resourceRegistration.registerReadWriteAttribute(CIPHER_SUITES, null, new SecurityDomainReloadWriteHandler(CIPHER_SUITES));
        resourceRegistration.registerReadWriteAttribute(ADDITIONAL_PROPERTIES, null, new SecurityDomainReloadWriteHandler(ADDITIONAL_PROPERTIES));

    }

    static class JSSEResourceDefinitionAdd extends SecurityDomainReloadAddHandler {
        static final JSSEResourceDefinitionAdd INSTANCE = new JSSEResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            KEYSTORE.validateAndSet(operation, model);
            TRUSTSTORE.validateAndSet(operation, model);
            KEYMANAGER.validateAndSet(operation, model);
            TRUSTMANAGER.validateAndSet(operation, model);
            CLIENT_ALIAS.validateAndSet(operation, model);
            SERVER_ALIAS.validateAndSet(operation, model);
            SERVICE_AUTH_TOKEN.validateAndSet(operation, model);
            CLIENT_AUTH.validateAndSet(operation, model);
            PROTOCOLS.validateAndSet(operation, model);
            CIPHER_SUITES.validateAndSet(operation, model);
            ADDITIONAL_PROPERTIES.validateAndSet(operation, model);
        }
    }

}

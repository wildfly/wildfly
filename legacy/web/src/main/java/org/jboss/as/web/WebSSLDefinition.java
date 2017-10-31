/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;


import java.util.List;

import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 12:26
 */
public class WebSSLDefinition extends ModelOnlyResourceDefinition {
    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING)
                    .setXmlName(Constants.NAME)
                    .setRequired(false)
                    .build();
    protected static final SimpleAttributeDefinition KEY_ALIAS =
            new SimpleAttributeDefinitionBuilder(Constants.KEY_ALIAS, ModelType.STRING)
                    .setRequired(false)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .build();
    protected static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.PASSWORD, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .build();
    protected static final SimpleAttributeDefinition CERTIFICATE_KEY_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.CERTIFICATE_KEY_FILE, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition CIPHER_SUITE =
            new SimpleAttributeDefinitionBuilder(Constants.CIPHER_SUITE, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("HIGH:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5"))
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition PROTOCOL =
            new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition VERIFY_CLIENT =
            new SimpleAttributeDefinitionBuilder(Constants.VERIFY_CLIENT, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition VERIFY_DEPTH =
            new SimpleAttributeDefinitionBuilder(Constants.VERIFY_DEPTH, ModelType.INT)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition CERTIFICATE_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.CERTIFICATE_FILE, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition CA_CERTIFICATE_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.CA_CERTIFICATE_FILE, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition CA_CERTIFICATE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.CA_CERTIFICATE_PASSWORD, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition CA_REVOCATION_URL =
            new SimpleAttributeDefinitionBuilder(Constants.CA_REVOCATION_URL, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition TRUSTSTORE_TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.TRUSTSTORE_TYPE, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition KEYSTORE_TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.KEYSTORE_TYPE, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition SESSION_CACHE_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.SESSION_CACHE_SIZE, ModelType.INT)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition SESSION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(Constants.SESSION_TIMEOUT, ModelType.INT)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition SSL_PROTOCOL =
            new SimpleAttributeDefinitionBuilder(Constants.SSL_PROTOCOL, ModelType.STRING)
                    .setRequired(false)
                    .setValidator(new StringLengthValidator(1, true))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static SimpleAttributeDefinition[] SSL_ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            KEY_ALIAS,
            PASSWORD,
            CERTIFICATE_KEY_FILE,
            CIPHER_SUITE,
            PROTOCOL,
            VERIFY_CLIENT,
            VERIFY_DEPTH,
            CERTIFICATE_FILE,
            CA_CERTIFICATE_FILE,
            CA_REVOCATION_URL,
            CA_CERTIFICATE_PASSWORD,
            KEYSTORE_TYPE,
            TRUSTSTORE_TYPE,
            SESSION_CACHE_SIZE,
            SESSION_TIMEOUT,
            SSL_PROTOCOL
    };

    protected static final WebSSLDefinition INSTANCE = new WebSSLDefinition();


    private List<AccessConstraintDefinition> accessConstraints;

    private WebSSLDefinition() {
        super(WebExtension.SSL_PATH,
                WebExtension.getResourceDescriptionResolver("connector.ssl"),
                SSL_ATTRIBUTES);
        SensitivityClassification sc = new SensitivityClassification(WebExtension.SUBSYSTEM_NAME, "web-ssl", false, true, true);
        this.accessConstraints = new SensitiveTargetAccessConstraintDefinition(sc).wrapAsList();
        setDeprecated(WebExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration ssl) {
        super.registerAttributes(ssl);
        ssl.registerReadOnlyAttribute(NAME, ReadResourceNameOperationStepHandler.INSTANCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}

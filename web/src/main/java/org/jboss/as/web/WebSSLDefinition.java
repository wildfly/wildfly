package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.alias.AbstractAliasedResourceDefinition;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
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
public class WebSSLDefinition extends AbstractAliasedResourceDefinition {
    protected static final WebSSLDefinition INSTANCE = new WebSSLDefinition();


    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING)
                    .setXmlName(Constants.NAME)
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition KEY_ALIAS =
            new SimpleAttributeDefinitionBuilder(Constants.KEY_ALIAS, ModelType.STRING)
                    .setXmlName(Constants.KEY_ALIAS)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.PASSWORD, ModelType.STRING)
                    .setXmlName(Constants.PASSWORD)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CERTIFICATE_KEY_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.CERTIFICATE_KEY_FILE, ModelType.STRING)
                    .setXmlName(Constants.CERTIFICATE_KEY_FILE)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CIPHER_SUITE =
            new SimpleAttributeDefinitionBuilder(Constants.CIPHER_SUITE, ModelType.STRING)
                    .setXmlName(Constants.CIPHER_SUITE)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition PROTOCOL =
            new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL, ModelType.STRING)
                    .setXmlName(Constants.PROTOCOL)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition VERIFY_CLIENT =
            new SimpleAttributeDefinitionBuilder(Constants.VERIFY_CLIENT, ModelType.STRING)
                    .setXmlName(Constants.VERIFY_CLIENT)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition VERIFY_DEPTH =
            new SimpleAttributeDefinitionBuilder(Constants.VERIFY_DEPTH, ModelType.INT)
                    .setXmlName(Constants.VERIFY_DEPTH)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CERTIFICATE_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.CERTIFICATE_FILE, ModelType.STRING)
                    .setXmlName(Constants.CERTIFICATE_FILE)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CA_CERTIFICATE_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.CA_CERTIFICATE_FILE, ModelType.STRING)
                    .setXmlName(Constants.CA_CERTIFICATE_FILE)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CA_CERTIFICATE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.CA_CERTIFICATE_PASSWORD, ModelType.STRING)
                    .setXmlName(Constants.CA_CERTIFICATE_PASSWORD)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CA_REVOCATION_URL =
            new SimpleAttributeDefinitionBuilder(Constants.CA_REVOCATION_URL, ModelType.STRING)
                    .setXmlName(Constants.CA_REVOCATION_URL)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition TRUSTSTORE_TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.TRUSTSTORE_TYPE, ModelType.STRING)
                    .setXmlName(Constants.TRUSTSTORE_TYPE)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition KEYSTORE_TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.KEYSTORE_TYPE, ModelType.STRING)
                    .setXmlName(Constants.KEYSTORE_TYPE)
                    .setAllowNull(true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition SESSION_CACHE_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.SESSION_CACHE_SIZE, ModelType.INT)
                    .setXmlName(Constants.SESSION_CACHE_SIZE)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition SESSION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(Constants.SESSION_TIMEOUT, ModelType.INT)
                    .setXmlName(Constants.SESSION_TIMEOUT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
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
            SESSION_TIMEOUT
        };


    private WebSSLDefinition() {
        super(WebExtension.SSL_PATH,
                WebExtension.getResourceDescriptionResolver("connector.ssl"),
                WebSSLAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration ssl) {
        ssl.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(NAME));
        for (AttributeDefinition attr : SSL_ATTRIBUTES) {
            ssl.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    public void registerAliasAttributes(ManagementResourceRegistration resourceRegistration, PathElement alias) {
        resourceRegistration.registerReadOnlyAttribute(NAME, aliasHandler);
        for (AttributeDefinition attr : SSL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, aliasHandler, aliasHandler);
        }
    }

}

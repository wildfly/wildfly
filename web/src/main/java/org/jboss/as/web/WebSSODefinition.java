package org.jboss.as.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.alias.AbstractAliasedResourceDefinition;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:54
 */
public class WebSSODefinition extends AbstractAliasedResourceDefinition {
    public static final WebSSODefinition INSTANCE = new WebSSODefinition();

    protected static final SimpleAttributeDefinition CACHE_CONTAINER =
            new SimpleAttributeDefinitionBuilder(Constants.CACHE_CONTAINER, ModelType.STRING, true)
                    .setXmlName(Constants.CACHE_CONTAINER)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CACHE_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.CACHE_NAME, ModelType.STRING, true)
                    .setXmlName(Constants.CACHE_NAME)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DOMAIN, ModelType.STRING, true)
                    .setXmlName(Constants.DOMAIN)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition REAUTHENTICATE =
            new SimpleAttributeDefinitionBuilder(Constants.REAUTHENTICATE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.REAUTHENTICATE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static SimpleAttributeDefinition[] SSO_ATTRIBUTES = {CACHE_CONTAINER, CACHE_NAME, DOMAIN, REAUTHENTICATE};

    private WebSSODefinition() {
        super(WebExtension.SSO_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.sso"),
                WebSSOAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration sso) {
        for (SimpleAttributeDefinition def : SSO_ATTRIBUTES) {
            sso.registerReadWriteAttribute(def, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(def));
        }
    }

    @Override
    public void registerAliasAttributes(ManagementResourceRegistration resourceRegistration, PathElement alias) {
        for (SimpleAttributeDefinition def : SSO_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, aliasHandler, aliasHandler);
        }
    }
}

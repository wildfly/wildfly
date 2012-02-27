package org.jboss.as.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.alias.AbstractAliasedResourceDefinition;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:33
 */
public class WebAccessLogDefinition extends AbstractAliasedResourceDefinition {
    public static final WebAccessLogDefinition INSTANCE = new WebAccessLogDefinition();

    protected static final SimpleAttributeDefinition PATTERN =
            new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
                    .setXmlName(Constants.PATTERN)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode("common"))
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition RESOLVE_HOSTS =
            new SimpleAttributeDefinitionBuilder(Constants.RESOLVE_HOSTS, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.RESOLVE_HOSTS)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition EXTENDED =
            new SimpleAttributeDefinitionBuilder(Constants.EXTENDED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.EXTENDED)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition PREFIX =
            new SimpleAttributeDefinitionBuilder(Constants.PREFIX, ModelType.STRING, true)
                    .setXmlName(Constants.PREFIX)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition ROTATE =
            new SimpleAttributeDefinitionBuilder(Constants.ROTATE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.ROTATE)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition[] ACCESS_LOG_ATTRIBUTES = {
            PATTERN,
            RESOLVE_HOSTS,
            EXTENDED,
            PREFIX,
            ROTATE
    };


    private WebAccessLogDefinition() {
        super(WebExtension.ACCESS_LOG_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.access-log"),
                WebAccessLogAdd.INSTANCE,
                WebAccessLogRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration accesslog) {
        for (SimpleAttributeDefinition def : ACCESS_LOG_ATTRIBUTES) {
            accesslog.registerReadWriteAttribute(def, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(def));
        }
    }

    @Override
    public void registerAliasAttributes(ManagementResourceRegistration resourceRegistration, PathElement alias) {
        for (SimpleAttributeDefinition def : ACCESS_LOG_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, aliasHandler, aliasHandler);
        }
    }
}

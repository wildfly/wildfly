package org.jboss.as.web;

import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:17
 */
public class WebVirtualHostDefinition extends SimpleResourceDefinition {
    public static final WebVirtualHostDefinition INSTANCE = new WebVirtualHostDefinition();

    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, true)
                    .setXmlName(Constants.NAME)
                    //.setAllowNull(false) //todo should be true
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    protected static final SimpleListAttributeDefinition ALIAS =
            SimpleListAttributeDefinition.Builder.of(Constants.ALIAS,
                    new SimpleAttributeDefinitionBuilder(Constants.ALIAS, ModelType.STRING, false)
                            .setXmlName(Constants.ALIAS)
                            .setAllowNull(false)
                            .setValidator(new StringLengthValidator(1, false))
                            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                            .build())
                    .setAllowNull(true)
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_WEB_MODULE =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_WEB_MODULE, ModelType.STRING, true)
                    .setXmlName(Constants.DEFAULT_WEB_MODULE)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(new StringLengthValidator(1, true,true))
                    .setDefaultValue(new ModelNode("ROOT.war"))
                    .build();
    protected static final SimpleAttributeDefinition ENABLE_WELCOME_ROOT =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLE_WELCOME_ROOT, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.ENABLE_WELCOME_ROOT)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();


    private WebVirtualHostDefinition() {
        super(WebExtension.HOST_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.VIRTUAL_SERVER),
                WebVirtualHostAdd.INSTANCE,
                WebVirtualHostRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration hosts) {
        hosts.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(NAME));
        hosts.registerReadWriteAttribute(ALIAS, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(ALIAS));
        // They excluded each other...
        hosts.registerReadWriteAttribute(ENABLE_WELCOME_ROOT, null, WriteEnableWelcomeRoot.INSTANCE);
        hosts.registerReadWriteAttribute(DEFAULT_WEB_MODULE, null, WriteDefaultWebModule.INSTANCE);


    }
}

package org.jboss.as.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
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
 * @created 23.2.12 16:34
 */
public class WebAccessLogDirectoryDefinition extends AbstractAliasedResourceDefinition {
    public static final WebAccessLogDirectoryDefinition INSTANCE = new WebAccessLogDirectoryDefinition();

    protected static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING, true)
                    .setXmlName(Constants.RELATIVE_TO)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("jboss.server.log.dir"))
                    .build();


    protected static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING, true)
                    .setXmlName(Constants.PATH)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();


    private WebAccessLogDirectoryDefinition() {
        super(WebExtension.DIRECTORY_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.access-log.directory"),
                WebAccessLogDirectoryAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration directory) {
        directory.registerReadWriteAttribute(RELATIVE_TO, null, new ReloadRequiredWriteAttributeHandler(RELATIVE_TO));
        directory.registerReadWriteAttribute(PATH, null, new ReloadRequiredWriteAttributeHandler(PATH));
    }

    @Override
    public void registerAliasAttributes(ManagementResourceRegistration resourceRegistration, PathElement alias) {
        resourceRegistration.registerReadWriteAttribute(RELATIVE_TO, aliasHandler, aliasHandler);
        resourceRegistration.registerReadWriteAttribute(PATH, aliasHandler, aliasHandler);
    }
}

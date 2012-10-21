package org.jboss.as.web;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Jean-Frederic Clere
 */
public class WebValveFileDefinition extends SimpleResourceDefinition {
    public static final WebValveFileDefinition INSTANCE = new WebValveFileDefinition();

    protected static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING)
                    .setXmlName(Constants.RELATIVE_TO)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();


    protected static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
                    .setXmlName(Constants.PATH)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .build();


    private WebValveFileDefinition() {
        super(WebExtension.FILE_PATH,
                WebExtension.getResourceDescriptionResolver("valve.file"),
                WebValveFileAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration directory) {
        directory.registerReadWriteAttribute(RELATIVE_TO, null, new ReloadRequiredWriteAttributeHandler(RELATIVE_TO));
        directory.registerReadWriteAttribute(PATH, null, new ReloadRequiredWriteAttributeHandler(PATH));
    }
}

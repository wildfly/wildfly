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
 * @author Tomaz Cerar
 * @created 23.2.12 17:13
 */
public class WebReWriteDefinition extends SimpleResourceDefinition {
    public static final WebReWriteDefinition INSTANCE = new WebReWriteDefinition();

    protected static final SimpleAttributeDefinition PATTERN =
            new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
                    .setXmlName(Constants.PATTERN)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, false))
                    .build();


    protected static final SimpleAttributeDefinition SUBSTITUTION =
            new SimpleAttributeDefinitionBuilder(Constants.SUBSTITUTION, ModelType.STRING, true)
                    .setXmlName(Constants.SUBSTITUTION)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, false))
                    .build();
    protected static final SimpleAttributeDefinition FLAGS =
            new SimpleAttributeDefinitionBuilder(Constants.FLAGS, ModelType.STRING, true)
                    .setXmlName(Constants.FLAGS)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, false))
                    .build();


    private WebReWriteDefinition() {
        super(WebExtension.REWRITE_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.rewrite"),
                WebReWriteAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration rewrite) {
        rewrite.registerReadWriteAttribute(PATTERN, null, new ReloadRequiredWriteAttributeHandler(PATTERN));
        rewrite.registerReadWriteAttribute(SUBSTITUTION, null, new ReloadRequiredWriteAttributeHandler(SUBSTITUTION));
        rewrite.registerReadWriteAttribute(FLAGS, null, new ReloadRequiredWriteAttributeHandler(FLAGS));
    }
}

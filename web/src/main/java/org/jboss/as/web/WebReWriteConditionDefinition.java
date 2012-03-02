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
 * @created 23.2.12 17:32
 */
public class WebReWriteConditionDefinition extends SimpleResourceDefinition {
    public static final WebReWriteConditionDefinition INSTANCE = new WebReWriteConditionDefinition();

    protected static final SimpleAttributeDefinition TEST =
            new SimpleAttributeDefinitionBuilder(Constants.TEST, ModelType.STRING, true)
                    .setXmlName(Constants.TEST)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, false))
                    .build();

    protected static final SimpleAttributeDefinition PATTERN =
            new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
                    .setXmlName(Constants.PATTERN)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, false))
                    .build();


    protected static final SimpleAttributeDefinition FLAGS =
            new SimpleAttributeDefinitionBuilder(Constants.FLAGS, ModelType.STRING, true)
                    .setXmlName(Constants.FLAGS)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, false))
                    .build();


    private WebReWriteConditionDefinition() {
        super(WebExtension.REWRITECOND_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.rewrite.condition"),
                WebReWriteConditionAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration rewritecondition) {
        rewritecondition.registerReadWriteAttribute(PATTERN, null, new ReloadRequiredWriteAttributeHandler(PATTERN));
        rewritecondition.registerReadWriteAttribute(TEST, null, new ReloadRequiredWriteAttributeHandler(TEST));
        rewritecondition.registerReadWriteAttribute(FLAGS, null, new ReloadRequiredWriteAttributeHandler(FLAGS));
    }
}

package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:03
 */
public class WebContainerDefinition extends SimpleResourceDefinition {
    public static final WebContainerDefinition INSTANCE = new WebContainerDefinition();

    protected static final SimpleListAttributeDefinition WELCOME_FILES =
            SimpleListAttributeDefinition.Builder.of(Constants.WELCOME_FILE,
                    new SimpleAttributeDefinitionBuilder(Constants.WELCOME_FILE, ModelType.STRING, true)
                            .setXmlName(Constants.WELCOME_FILE)
                            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                            .setValidator(new StringLengthValidator(1, true))
                            .build())
                    .setAllowNull(true)
                    .build();
    protected static final PropertiesAttributeDefinition MIME_MAPPINGS = new PropertiesAttributeDefinition(Constants.MIME_MAPPING, Constants.MIME_MAPPING, true);

    private static final SimpleAttributeDefinition MIME_NAME = new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    private static final SimpleAttributeDefinition MIME_VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    protected static final AttributeDefinition[] CONTAINER_ATTRIBUTES={
            WELCOME_FILES,
            MIME_MAPPINGS,
    };
    private static final OperationDefinition ADD_MIME = new SimpleOperationDefinition("add-mime", WebExtension.getResourceDescriptionResolver("container.mime-mapping"), MIME_NAME, MIME_VALUE);
    private static final OperationDefinition REMOVE_MIME = new SimpleOperationDefinitionBuilder("remove-mime", WebExtension.getResourceDescriptionResolver("container.mime-mapping"))
               .addParameter(MIME_NAME)
               .build();

    private WebContainerDefinition() {
        super(WebExtension.CONTAINER_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.CONTAINER),
                WebContainerAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }


    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration container) {
        super.registerOperations(container);
        container.registerOperationHandler(ADD_MIME,MimeMappingAdd.INSTANCE);
        container.registerOperationHandler(REMOVE_MIME,MimeMappingRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration container) {
        container.registerReadWriteAttribute(WELCOME_FILES, null, new ReloadRequiredWriteAttributeHandler(WELCOME_FILES));
        container.registerReadWriteAttribute(MIME_MAPPINGS, null, new ReloadRequiredWriteAttributeHandler(MIME_MAPPINGS));
    }

}

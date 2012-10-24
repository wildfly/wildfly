package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jean-Frederic Clere
 */
public class WebValveDefinition extends SimpleResourceDefinition {
    protected static final WebValveDefinition INSTANCE = new WebValveDefinition();

    protected static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(Constants.MODULE,
            ModelType.STRING).setXmlName(Constants.MODULE).setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setValidator(new StringLengthValidator(1)).build();

    protected static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder(Constants.CLASS_NAME,
            ModelType.STRING).setXmlName(Constants.CLASS_NAME).setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setValidator(new StringLengthValidator(1)).build();

    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
                    .setXmlName(Constants.ENABLED)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition[] ATTRIBUTES = { MODULE, CLASS_NAME, ENABLED};

    protected static final PropertiesAttributeDefinition PARAMS = new PropertiesAttributeDefinition(Constants.PARAM,
            Constants.PARAM, true);

    private static final SimpleAttributeDefinition PARAM_NAME = new SimpleAttributeDefinitionBuilder(Constants.PARAM_NAME,
            ModelType.STRING, true).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true)).build();

    private static final SimpleAttributeDefinition PARAM_VALUE = new SimpleAttributeDefinitionBuilder(Constants.PARAM_VALUE,
            ModelType.STRING, true).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true)).build();

    protected static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
            .setXmlName(Constants.PATH)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true, true))
            .build();

    protected static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING)
                    .setXmlName(Constants.RELATIVE_TO)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("jboss.server.log.dir"))
                    .build();

    private static final OperationDefinition ADD_PARAM = new SimpleOperationDefinitionBuilder("add-param", WebExtension.getResourceDescriptionResolver("valve.param"))
    .setParameters(PARAM_NAME, PARAM_VALUE)
    .build();
private static final OperationDefinition REMOVE_PARAM = new SimpleOperationDefinitionBuilder("remove-param", WebExtension.getResourceDescriptionResolver("valve.param"))
       .addParameter(PARAM_NAME)
       .build();

    private WebValveDefinition() {
        super(WebExtension.VALVE_PATH, WebExtension.getResourceDescriptionResolver(Constants.VALVE), WebValveAdd.INSTANCE,
                WebValveRemove.INSTANCE);
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration container) {
        super.registerOperations(container);
        container.registerOperationHandler(ADD_PARAM,WebValveParamAdd.INSTANCE);
        container.registerOperationHandler(REMOVE_PARAM,WebValveParamRemove.INSTANCE);
    }
    @Override
    public void registerAttributes(ManagementResourceRegistration valves) {
        for (AttributeDefinition def : ATTRIBUTES) {
            valves.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
        valves.registerReadWriteAttribute(PARAMS, null, new ReloadRequiredWriteAttributeHandler(PARAMS));
    }
}

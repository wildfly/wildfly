package org.jboss.as.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceRemoveDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * @author Tomaz Cerar
 * @created 22.2.12 14:29
 */
public class WebDefinition extends SimpleResourceDefinition {
    public static final WebDefinition INSTANCE = new WebDefinition();

    protected static final SimpleAttributeDefinition DEFAULT_VIRTUAL_SERVER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_VIRTUAL_SERVER, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(Constants.DEFAULT_VIRTUAL_SERVER)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default-host"))
                    .build();
    protected static final SimpleAttributeDefinition NATIVE =
            new SimpleAttributeDefinitionBuilder(Constants.NATIVE, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setXmlName(Constants.NATIVE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition INSTANCE_ID =
                new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING, true)
                        .setAllowExpression(true)
                        .setXmlName(Constants.INSTANCE_ID)
                        .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                        .setDefaultValue(null)
                        .build();

    private WebDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, WebExtension.SUBSYSTEM_NAME),
                WebExtension.getResourceDescriptionResolver(null));
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();
        // Ops to add and remove the root resource
        final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
        rootResourceRegistration.registerOperationHandler(ADD, WebSubsystemAdd.INSTANCE, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        final DescriptionProvider subsystemRemoveDescription = new DefaultResourceRemoveDescriptionProvider(rootResolver);
        rootResourceRegistration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, subsystemRemoveDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadWriteAttribute(DEFAULT_VIRTUAL_SERVER, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(DEFAULT_VIRTUAL_SERVER));
        registration.registerReadWriteAttribute(NATIVE, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(NATIVE));
        registration.registerReadWriteAttribute(INSTANCE_ID, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(INSTANCE_ID));
    }
}

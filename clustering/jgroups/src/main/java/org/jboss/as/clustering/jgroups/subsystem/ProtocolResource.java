package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for /subsystem=jgroups/stack=X/protocol=Y
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

public class ProtocolResource extends SimpleResourceDefinition {

    static final PathElement PROTOCOL_PATH = PathElement.pathElement(ModelKeys.PROTOCOL);
    static final ProtocolResource INSTANCE = new ProtocolResource();

    // attributes
    static SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TYPE, ModelType.STRING, false)
                    .setXmlName(Attribute.TYPE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static SimpleAttributeDefinition SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_BINDING, ModelType.STRING, true)
                    .setXmlName(Attribute.SOCKET_BINDING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, ModelType.PROPERTY, true);
    static SimpleListAttributeDefinition PROPERTIES = SimpleListAttributeDefinition.Builder.of(ModelKeys.PROPERTIES, PROPERTY).
            setAllowNull(true).
            build();

    static AttributeDefinition[] PROTOCOL_ATTRIBUTES = new AttributeDefinition[] {TYPE, SOCKET_BINDING};
    static AttributeDefinition[] PROTOCOL_PARAMETERS = new AttributeDefinition[] {TYPE, SOCKET_BINDING, PROPERTIES};

    static final ObjectTypeAttributeDefinition PROTOCOL = ObjectTypeAttributeDefinition.
                Builder.of(ModelKeys.PROTOCOL, PROTOCOL_ATTRIBUTES).
                setAllowNull(true).
                setSuffix(null).
                setSuffix("protocol").
                build();

    static final ObjectListAttributeDefinition PROTOCOLS = ObjectListAttributeDefinition.
            Builder.of(ModelKeys.PROTOCOLS, PROTOCOL).
            setAllowNull(true).
            build();


    static final OperationStepHandler PROTOCOL_ADD = new ProtocolLayerAdd(PROTOCOL_PARAMETERS);
    static final OperationStepHandler PROTOCOL_REMOVE = new ProtocolLayerRemove();


    // registration
    ProtocolResource() {
        super(PROTOCOL_PATH,
                JGroupsExtension.getResourceDescriptionResolver(ModelKeys.PROTOCOL),
                null,
                null);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(PROTOCOL_ATTRIBUTES);

        for (AttributeDefinition attr : PROTOCOL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        resourceRegistration.registerSubModel(PropertyResource.INSTANCE);
    }

    // descriptions
    static final DescriptionProvider PROTOCOL_ADD_DESCRIPTOR = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return getProtocolAddDescription(locale);
        }
    };

    static final DescriptionProvider PROTOCOL_REMOVE_DESCRIPTOR = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return getProtocolRemoveDescription(locale);
        }
    };

    static final DescriptionProvider EXPORT_NATIVE_CONFIGURATION_DESCRIPTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return getExportNativeConfigurationDescription(locale);
        }
    };

    static ModelNode getProtocolAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_PROTOCOL, resources, "stack.add-protocol");
        for (AttributeDefinition attr : PROTOCOL_PARAMETERS) {
            attr.addOperationParameterDescription(resources, "stack.add-protocol", op);
        }
        return op;
    }

    static ModelNode getProtocolRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.REMOVE_PROTOCOL, resources, "stack.remove-protocol");
        ProtocolResource.TYPE.addOperationParameterDescription(resources, "stack.remove-protocol", op);
        return op;
    }

    static ModelNode getExportNativeConfigurationDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.EXPORT_NATIVE_CONFIGURATION, resources, "stack.export-native-configuration");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        return op;
    }


    private static ResourceBundle getResources(Locale locale) {
        return ResourceBundle.getBundle(JGroupsExtension.RESOURCE_NAME, (locale == null) ? Locale.getDefault() : locale);
    }

    private static ModelNode createDescription(ResourceBundle resources, String key) {
        return createOperationDescription(null, resources, key);
    }

    private static ModelNode createOperationDescription(String operation, ResourceBundle resources, String key) {
        ModelNode description = new ModelNode();
        if (operation != null) {
            description.get(ModelDescriptionConstants.OPERATION_NAME).set(operation);
        }
        description.get(ModelDescriptionConstants.DESCRIPTION).set(resources.getString(key));
        return description;
    }

}

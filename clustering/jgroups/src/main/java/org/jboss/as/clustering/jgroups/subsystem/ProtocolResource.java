package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
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

    // operations
    static final OperationDefinition PROTOCOL_ADD = new SimpleOperationDefinitionBuilder(ModelKeys.ADD_PROTOCOL, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setParameters(PROTOCOL_PARAMETERS)
            .build();

    static final OperationDefinition PROTOCOL_REMOVE = new SimpleOperationDefinitionBuilder(ModelKeys.REMOVE_PROTOCOL, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setParameters(TYPE)
            .build();

    static final OperationStepHandler PROTOCOL_ADD_HANDLER = new ProtocolLayerAdd(PROTOCOL_PARAMETERS);
    static final OperationStepHandler PROTOCOL_REMOVE_HANDLER = new ProtocolLayerRemove();

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
}

package org.jboss.as.undertow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowRootDefinition extends SimplePersistentResourceDefinition {
    public static final UndertowRootDefinition INSTANCE = new UndertowRootDefinition();
    protected static final SimpleAttributeDefinition DEFAULT_VIRTUAL_HOST =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_VIRTUAL_HOST, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default-host"))
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_SERVLET_CONTAINER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SERVLET_CONTAINER, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default"))
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_SERVER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SERVER, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default-server"))
                    .build();


    protected static final SimpleAttributeDefinition INSTANCE_ID =
            new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static AttributeDefinition[] ATTRIBUTES = {DEFAULT_VIRTUAL_HOST, DEFAULT_SERVLET_CONTAINER, DEFAULT_SERVER, INSTANCE_ID};


    private UndertowRootDefinition() {
        super(UndertowExtension.SUBSYSTEM_PATH,
                UndertowExtension.getResolver(),
                UndertowSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

}

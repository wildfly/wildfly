package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.wildfly.extension.undertow.errorhandler.ErrorHandlerDefinitions;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class UndertowRootDefinition extends SimplePersistentResourceDefinition {
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
    static final AttributeDefinition[] ATTRIBUTES = {DEFAULT_VIRTUAL_HOST, DEFAULT_SERVLET_CONTAINER, DEFAULT_SERVER, INSTANCE_ID};
    static final PersistentResourceDefinition[] CHILDREN = {
            BufferCacheDefinition.INSTANCE,
            ServerDefinition.INSTANCE,
            ServletContainerDefinition.INSTANCE,
            ErrorHandlerDefinitions.INSTANCE,
            HandlerDefinitions.INSTANCE,
            FilterDefinitions.INSTANCE
    };


    private UndertowRootDefinition() {
        super(UndertowExtension.SUBSYSTEM_PATH,
                UndertowExtension.getResolver(),
                UndertowSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return Arrays.asList(CHILDREN);
    }
}

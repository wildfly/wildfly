package org.jboss.as.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ServerDefinition extends SimplePersistentResourceDefinition {
    static final ServerDefinition INSTANCE = new ServerDefinition();
    static final SimpleAttributeDefinition DEFAULT_HOST = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_HOST, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("default-host"))
            .build();
    static final SimpleAttributeDefinition SERVLET_CONTAINER = new SimpleAttributeDefinitionBuilder(Constants.SERVLET_CONTAINER, ModelType.STRING)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("default"))
            .build();
    static final List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(DEFAULT_HOST, SERVLET_CONTAINER);
    static final List<SimplePersistentResourceDefinition> CHILDREN = Arrays.asList(
            AJPListenerResourceDefinition.INSTANCE,
            HttpListenerResourceDefinition.INSTANCE,
            HttpsListenerResourceDefinition.INSTANCE,
            HostDefinition.INSTANCE);

    public ServerDefinition() {
        super(UndertowExtension.SERVER_PATH, UndertowExtension.getResolver(Constants.SERVER), new ServerAdd(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return (Collection) ATTRIBUTES;
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        //noinspection unchecked
        return CHILDREN;
    }

}

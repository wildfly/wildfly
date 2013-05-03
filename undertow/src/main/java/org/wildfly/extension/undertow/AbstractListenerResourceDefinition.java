package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
abstract class AbstractListenerResourceDefinition extends SimplePersistentResourceDefinition {
    protected static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();
    protected static final SimpleAttributeDefinition BUFFER_POOL = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_POOL, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();
    protected static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();
    protected static AttributeDefinition[] ATTRIBUTES = {SOCKET_BINDING, WORKER, BUFFER_POOL, ENABLED};


    public AbstractListenerResourceDefinition(PathElement pathElement) {
        super(pathElement,
                UndertowExtension.getResolver(pathElement.getKey()),
                null,
                null
        );
    }

    static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        super.registerAddOperation(resourceRegistration, getAddHandler(), OperationEntry.Flag.RESTART_NONE);
        super.registerRemoveOperation(resourceRegistration, new ListenerRemoveHandler(getAddHandler()), OperationEntry.Flag.RESTART_NONE);
    }

    protected abstract AbstractListenerAdd getAddHandler();
}

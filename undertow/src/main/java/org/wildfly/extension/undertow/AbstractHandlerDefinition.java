package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAddHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public abstract class AbstractHandlerDefinition extends SimplePersistentResourceDefinition {
    protected final String name;

    protected AbstractHandlerDefinition(final String name, AbstractAddStepHandler addHandler, AbstractRemoveStepHandler removeHandler) {
        super(PathElement.pathElement(name), UndertowExtension.getResolver(Constants.HANDLER, name), addHandler, removeHandler);
        this.name = name;
    }

    protected AbstractHandlerDefinition(final String name) {
        super(PathElement.pathElement(name), UndertowExtension.getResolver(Constants.HANDLER, name));
        this.name = name;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD) == null) {
            registerAddOperation(resourceRegistration, new DefaultAddHandler(getAttributes()), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.REMOVE) == null) {
            registerRemoveOperation(resourceRegistration, new DefaultHandlerRemove(), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public String getXmlElementName() {
        return this.name;
    }

    protected static class DefaultHandlerRemove extends AbstractRemoveStepHandler {
        private DefaultHandlerRemove() {

        }
    }

}

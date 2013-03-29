package org.jboss.as.undertow;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public abstract class AbstractHandlerResourceDefinition extends SimplePersistentResourceDefinition implements Handler {
    protected final String name;

    protected AbstractHandlerResourceDefinition(final String name, AbstractAddStepHandler addHandler, AbstractRemoveStepHandler removeHandler) {
        super(PathElement.pathElement(Constants.HANDLER, name), UndertowExtension.getResolver(Constants.HANDLER, name), addHandler, removeHandler);
        this.name = name;
    }

    protected AbstractHandlerResourceDefinition(final String name) {
        super(PathElement.pathElement(Constants.HANDLER, name), UndertowExtension.getResolver(Constants.HANDLER, name));
        this.name = name;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD) == null) {
            registerAddOperation(resourceRegistration, new DefaultHandlerAdd(), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.REMOVE) == null) {
            registerRemoveOperation(resourceRegistration, new DefaultHandlerRemove(), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        ReloadRequiredWriteAttributeHandler writeHandler = new ReloadRequiredWriteAttributeHandler(getAttributes());
        for (AttributeDefinition def : getAttributes()) {
            resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    protected boolean useValueAsElementName() {
        return true;
    }

    protected class DefaultHandlerAdd extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : getAttributes()) {
                def.validateAndSet(operation, model);
            }
        }
    }

    protected static class DefaultHandlerRemove extends AbstractRemoveStepHandler {
        private DefaultHandlerRemove() {

        }
    }

}

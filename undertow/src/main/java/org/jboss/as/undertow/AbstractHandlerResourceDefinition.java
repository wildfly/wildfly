package org.jboss.as.undertow;

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
    public AttributeDefinition[] getAttributes() {
        return new AttributeDefinition[0];
    }

    @Override
    protected boolean useValueAsElementName() {
        return true;
    }
    /*

    protected Map<String, AttributeDefinition> getAttributeMap() {
        Map<String, AttributeDefinition> res = new HashMap<>();
        for (AttributeDefinition def : getAttributes()) {
            res.put(def.getName(), def);
        }
        return res;
    }

    public void parse(final XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = parentAddress.append(Constants.HANDLER, getName());
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        Map<String, AttributeDefinition> attributes = getAttributeMap();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attributeName = reader.getAttributeLocalName(i);
            if (attributes.containsKey(attributeName)) {
                String value = reader.getAttributeValue(i);
                AttributeDefinition def = attributes.get(attributeName);
                if (def instanceof SimpleAttributeDefinition) {
                    ((SimpleAttributeDefinition) def).parseAndSetParameter(value, op, reader);
                } else {
                    throw new IllegalArgumentException("we should know how to handle " + def);
                }
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }

        }
        ParseUtils.requireNoContent(reader);
    }

    @Override
    public void persist(XMLExtendedStreamWriter writer, Property handler) throws XMLStreamException {
        writer.writeStartElement(getName());
        for (AttributeDefinition def : getAttributes()) {
            def.getAttributeMarshaller().marshallAsAttribute(def, handler.getValue(), false, writer);
        }
        writer.writeEndElement();
    }*/

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

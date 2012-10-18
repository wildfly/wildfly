package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Implements the operation /subsystem=jgroups/stack=X/protocol=Y:add()
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ProtocolLayerAdd implements OperationStepHandler {

    AttributeDefinition[] attributes ;

    public ProtocolLayerAdd(AttributeDefinition... attributes) {
        this.attributes = attributes ;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // read /subsystem=jgroups/stack=* for update
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();

        // validate the protocol type to be added
        ModelNode type = ProtocolResource.TYPE.validateOperation(operation);
        PathElement protocolRelativePath = PathElement.pathElement(ModelKeys.PROTOCOL, type.asString());

        // if child resource already exists, throw OFE
        if (resource.hasChild(protocolRelativePath))  {
            throw JGroupsMessages.MESSAGES.protocolAlreadyDefined(protocolRelativePath.toString()) ;
        }

        // now get the created model
        Resource childResource = context.createResource(PathAddress.pathAddress(protocolRelativePath));
        final ModelNode protocol = childResource.getModel();

        // Process attributes
        for(final AttributeDefinition attribute : attributes) {
            // we use PROPERTIES only to allow the user to pass in a list of properties on store add commands
            // don't copy these into the model
            if (attribute.getName().equals(ModelKeys.PROPERTIES))
                continue ;

            attribute.validateAndSet(operation, protocol);
        }

        // get the current list of protocol names and add the new protocol
        // this list is used to maintain order
        ModelNode protocols = subModel.get(ModelKeys.PROTOCOLS) ;
        if (!protocols.isDefined()) {
            protocols.setEmptyList();
        }
        protocols.add(type);

        // Process type specific properties if required
        process(subModel, operation);

        // The protocol parameters  <property name=>value</property>
        if(operation.hasDefined(ModelKeys.PROPERTIES)) {
            for(Property property : operation.get(ModelKeys.PROPERTIES).asPropertyList()) {
                // create a new property=name resource
                final Resource param = context.createResource(PathAddress.pathAddress(protocolRelativePath, PathElement.pathElement(ModelKeys.PROPERTY, property.getName())));
                final ModelNode value = property.getValue();
                if(! value.isDefined()) {
                    throw JGroupsMessages.MESSAGES.propertyNotDefined(property.getName(), protocolRelativePath.toString());
                }
                // set the value of the property
                param.getModel().get(ModelDescriptionConstants.VALUE).set(value);
            }
        }
        // This needs a reload
        reloadRequiredStep(context);
        context.stepCompleted();
    }

    void process(ModelNode subModel, ModelNode operation) {
        //
    }

    /**
     * Add a step triggering the {@linkplain org.jboss.as.controller.OperationContext#reloadRequired()} in case the
     * the cache service is installed, since the transport-config operations need a reload/restart and can't be
     * applied to the runtime directly.
     *
     * @param context the operation context
     */
    void reloadRequiredStep(final OperationContext context) {
        if (context.getProcessType().isServer() &&  !context.isBooting()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    // add some condition here if reload needs to be conditional on context
                    // e.g. if a service is not installed, don't do a reload
                    context.reloadRequired();
                    context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}

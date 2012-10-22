package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.controller.AbstractAddStepHandler;
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
 * Operation handler for /subsystem=jgroups/stack=X/transport=TRANSPORT:add()
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportLayerAdd implements OperationStepHandler {

    AttributeDefinition[] attributes ;

    public TransportLayerAdd(AttributeDefinition... attributes) {
        this.attributes = attributes ;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final PathElement transportRelativePath = PathElement.pathElement(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();

        // Process attributes
        for(final AttributeDefinition attribute : attributes) {
            // don't process properties twice - we do them below
            if (attribute.getName().equals(ModelKeys.PROPERTIES))
                continue ;
            attribute.validateAndSet(operation, subModel);
        }

        // The transport config parameters  <property name=>value</property>
        if(operation.hasDefined(ModelKeys.PROPERTIES)) {
            for(Property property : operation.get(ModelKeys.PROPERTIES).asPropertyList()) {
                // create a new property=name resource
                final Resource param = context.createResource(PathAddress.pathAddress(PathElement.pathElement(ModelKeys.PROPERTY, property.getName())));
                final ModelNode value = property.getValue();
                if(!value.isDefined()) {
                    throw JGroupsMessages.MESSAGES.propertyNotDefined(property.getName(), transportRelativePath.toString());
                }
                // set the value of the property
                param.getModel().get(ModelDescriptionConstants.VALUE).set(value);
            }
        }
        // This needs a reload
        reloadRequiredStep(context);
        context.stepCompleted();
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

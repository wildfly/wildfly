package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import static org.jboss.as.clustering.jgroups.subsystem.CommonAttributes.PROTOCOL;
import static org.jboss.as.clustering.jgroups.subsystem.CommonAttributes.PROTOCOL_ATTRIBUTES;
import static org.jboss.as.clustering.jgroups.subsystem.CommonAttributes.TRANSPORT_ATTRIBUTES;
import static org.jboss.as.clustering.jgroups.subsystem.CommonAttributes.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Common code for handling the following stack configuration elements
 * {transport, protocol}
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StackConfigOperationHandlers {

    /** The stack transport config add operation handler. */
    static final OperationStepHandler TRANSPORT_ADD = new TransportConfigAdd(TRANSPORT_ATTRIBUTES) {
        public void process(ModelNode submodel , ModelNode operation){
          // override transport stuff here
        }
    };
    static final SelfRegisteringAttributeHandler TRANSPORT_ATTR = new AttributeWriteHandler(TRANSPORT_ATTRIBUTES);

    /** The stack protocol config add operation handler. */
    static final OperationStepHandler PROTOCOL_ADD = new ProtocolConfigAdd(PROTOCOL_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler PROTOCOL_ATTR = new AttributeWriteHandler(PROTOCOL_ATTRIBUTES);

    /** The protocol remove operation handler. */
    static final OperationStepHandler PROTOCOL_REMOVE = new ProtocolConfigRemove();

    /** The transport remove operation handler. */
    static final OperationStepHandler REMOVE = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    /** The cache config property add operation handler. */
    static final OperationStepHandler PROTOCOL_PROPERTY_ADD = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            CommonAttributes.VALUE.validateAndSet(operation, resource.getModel());
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    static final OperationStepHandler PROTOCOL_PROPERTY_ATTR = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            CommonAttributes.VALUE.validateAndSet(operation, resource.getModel());
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    /**
     * Helper class to process adding nested stack transport configuration element to the stack parent resource.
     * Override the process method in order to process configuration specific elements.
     */
    private static class TransportConfigAdd implements OperationStepHandler {
        private final AttributeDefinition[] attributes;

        TransportConfigAdd(final AttributeDefinition[] attributes) {
            this.attributes = attributes;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();

            // Process attributes
            for(final AttributeDefinition attribute : attributes) {
                // don't process properties twice - we do them below
                if (attribute.getName().equals(ModelKeys.PROPERTIES))
                    continue ;

                attribute.validateAndSet(operation, subModel);
            }

            // Process type specific properties if required
            process(subModel, operation);

            // The transport config parameters  <property name=>value</property>
            if(operation.hasDefined(ModelKeys.PROPERTIES)) {
                for(Property property : operation.get(ModelKeys.PROPERTIES).asPropertyList()) {
                    // create a new property=name resource
                    final Resource param = context.createResource(PathAddress.pathAddress(PathElement.pathElement(ModelKeys.PROPERTY, property.getName())));
                    final ModelNode value = property.getValue();
                    if(! value.isDefined()) {
                        throw new OperationFailedException(new ModelNode().set("property " + property.getName() + " not defined"));
                    }
                    // set the value of the property
                    param.getModel().get(ModelDescriptionConstants.VALUE).set(value);
                }
            }
            // This needs a reload
            reloadRequiredStep(context);
            context.completeStep();
        }

        void process(ModelNode subModel, ModelNode operation) {
            //
        };
    }

    /**
     * Helper class to process adding nested stack protocol configuration elements to the stack parent resource.
     * Override the process method in order to process configuration specific elements.
     */
    private static class ProtocolConfigAdd implements OperationStepHandler {
        private final AttributeDefinition[] attributes;

        ProtocolConfigAdd(final AttributeDefinition[] attributes) {
            this.attributes = attributes;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            // read /subsystem=jgroups/stack=* for update
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();

            // validate the protocol type to be added
            ModelNode type = TYPE.validateOperation(operation);
            PathElement protocolRelativePath = PathElement.pathElement(ModelKeys.PROTOCOL, type.asString());

            // if child resource already exists, throw OFE
            if (resource.hasChild(protocolRelativePath))  {
                throw new OperationFailedException(new ModelNode().set("protocol with relative path " + protocolRelativePath.toString() +  " is already defined"));
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
                        throw new OperationFailedException(new ModelNode().set("property " + property.getName() + " not defined"));
                    }
                    // set the value of the property
                    param.getModel().get(ModelDescriptionConstants.VALUE).set(value);
                }
            }
            // This needs a reload
            reloadRequiredStep(context);
            context.completeStep();
        }

        void process(ModelNode subModel, ModelNode operation) {
            //
        };
    }

    /**
     * Helper class to process removing nested stack protocol configuration elements to the stack parent resource.
     */
    private static class ProtocolConfigRemove implements OperationStepHandler {

        ProtocolConfigRemove() {
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            // read /subsystem=jgroups/stack=* for update
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();

             // validate the protocol type to be added
            ModelNode type = TYPE.validateOperation(operation);
            PathElement protocolRelativePath = PathElement.pathElement(ModelKeys.PROTOCOL, type.asString());

            // if child resource already exists, throw OFE
            // TODO not sure if this works ex expected - it may only confirm a registered resource
            if (!resource.hasChild(protocolRelativePath))  {
                throw new OperationFailedException(new ModelNode().set("protocol with relative path " + protocolRelativePath.toString() +  " is not defined"));
            }

            // remove the resource and its children
            context.removeResource(PathAddress.pathAddress(protocolRelativePath));

            // get the current list of protocol names and remove the protocol
            // copy all elements of the list except the one to remove
            // this list is used to maintain order
            List<ModelNode> protocols = subModel.get(ModelKeys.PROTOCOLS).asList() ;
            ModelNode newList = new ModelNode() ;
            if (protocols == null) {
                // something wrong
            }
            for (ModelNode protocol : protocols) {
                if (!protocol.asString().equals(type.asString())) {
                   newList.add(protocol);
                }
            }
            subModel.get(ModelKeys.PROTOCOLS).set(newList);

            // This needs a reload
            reloadRequiredStep(context);
            context.completeStep();
        }

        void process(ModelNode subModel, ModelNode operation) {
            //
        };
    }


    interface SelfRegisteringAttributeHandler extends OperationStepHandler {
        void registerAttributes(final ManagementResourceRegistration registry);
    }

    /**
     * Helper class to handle write access as well as register attributes.
     */
    static class AttributeWriteHandler extends ReloadRequiredWriteAttributeHandler implements SelfRegisteringAttributeHandler {
        final AttributeDefinition[] attributes;

        private AttributeWriteHandler(AttributeDefinition[] attributes) {
            super(attributes);
            this.attributes = attributes;
        }

        public void registerAttributes(final ManagementResourceRegistration registry) {
            final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
            for (AttributeDefinition attr : attributes) {
                registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
            }
        }
    }

    static ModelNode createOperation(AttributeDefinition[] attributes, ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    static ModelNode createProtocolOperation(AttributeDefinition[] attributes, ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ModelKeys.ADD_PROTOCOL, address);
        for(final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    /**
     * Add a step triggering the {@linkplain org.jboss.as.controller.OperationContext#reloadRequired()} in case the
     * the cache service is installed, since the transport-config operations need a reload/restart and can't be
     * applied to the runtime directly.
     *
     * @param context the operation context
     */
    static void reloadRequiredStep(final OperationContext context) {
        if (context.getProcessType().isServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    // add some condition here if reload needs to be conditional on context
                    // e.g. if a service is not installed, don't do a reload
                    context.reloadRequired();
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}

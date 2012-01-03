package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.EnumSet;
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
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Common code for handling the following cache configuration elements
 * {locking, transaction, eviction, expiration, state-transfer, rehashing, store, file-store, jdbc-store, remote-store}
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigOperationHandlers {


    /** The cache locking config add operation handler. */
    static final OperationStepHandler LOCKING_ADD = new BasicCacheConfigAdd(CommonAttributes.LOCKING_ATTRIBUTES) {
        public void process(ModelNode submodel , ModelNode operation){
          // override locking stuff here
        }
    };
    static final SelfRegisteringAttributeHandler LOCKING_ATTR = new AttributeWriteHandler(CommonAttributes.LOCKING_ATTRIBUTES);

    /** The cache transaction config add operation handler. */
    static final OperationStepHandler TRANSACTION_ADD = new BasicCacheConfigAdd(CommonAttributes.TRANSACTION_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler TRANSACTION_ATTR = new AttributeWriteHandler(CommonAttributes.TRANSACTION_ATTRIBUTES);

    /** The cache eviction config add operation handler. */
    static final OperationStepHandler EVICTION_ADD = new BasicCacheConfigAdd(CommonAttributes.EVICTION_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler EVICTION_ATTR = new AttributeWriteHandler(CommonAttributes.EVICTION_ATTRIBUTES);

    /** The cache expiration config add operation handler. */
    static final OperationStepHandler EXPIRATION_ADD = new BasicCacheConfigAdd(CommonAttributes.EXPIRATION_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler EXPIRATION_ATTR = new AttributeWriteHandler(CommonAttributes.EXPIRATION_ATTRIBUTES);

    /** The cache state transfer config add operation handler. */
    static final OperationStepHandler STATE_TRANSFER_ADD = new BasicCacheConfigAdd(CommonAttributes.STATE_TRANSFER_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler STATE_TRANSFER_ATTR = new AttributeWriteHandler(CommonAttributes.STATE_TRANSFER_ATTRIBUTES);

    /** The cache rehashing config add operation handler. */
    static final OperationStepHandler REHASHING_ADD = new BasicCacheConfigAdd(CommonAttributes.REHASHING_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler REHASHING_ATTR = new AttributeWriteHandler(CommonAttributes.REHASHING_ATTRIBUTES);

    /** The cache store config add operation handler. */
    static final OperationStepHandler STORE_ADD = new BasicCacheConfigAdd(CommonAttributes.STORE_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler STORE_ATTR = new AttributeWriteHandler(CommonAttributes.STORE_ATTRIBUTES);

    /** The cache config remove operation handler. */
    static final OperationStepHandler REMOVE = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    /** The cache config property add operation handler. */
    static final OperationStepHandler STORE_PROPERTY_ADD = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            CommonAttributes.VALUE.validateAndSet(operation, resource.getModel());
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    static final OperationStepHandler STORE_PROPERTY_ATTR = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            CommonAttributes.VALUE.validateAndSet(operation, resource.getModel());
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    /**
     * Helper class to process adding nested cache configuration elements to the cache parent resource.
     * Override the process method in order to process configuration specific elements.
     *
     */
    private static class BasicCacheConfigAdd implements OperationStepHandler, DescriptionProvider {
        private final AttributeDefinition[] attributes;

        BasicCacheConfigAdd(final AttributeDefinition[] attributes) {
            this.attributes = attributes;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();

            // Process attributes
            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, subModel);
            }

            // Process acceptor/connector type specific properties
            process(subModel, operation);

            // The cache config parameters  <property name=>value</property>
            if(operation.hasDefined(ModelKeys.PROPERTY)) {
                for(Property property : operation.get(ModelKeys.PROPERTY).asPropertyList()) {
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

        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode().set(DESCRIPTION);
        }

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

    /**
     * Add a step triggering the {@linkplain org.jboss.as.controller.OperationContext#reloadRequired()} in case the
     * the cache service is installed, since the transport-config operations need a reload/restart and can't be
     * applied to the runtime directly.
     *
     * @param context the operation context
     */
    static void reloadRequiredStep(final OperationContext context) {
        if(context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                    // shorten this by providing static getServiceName methods which take an OP_ADDR
                    PathAddress elementAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
                    PathAddress cacheAddress = elementAddress.subAddress(0, elementAddress.size() - 1);
                    PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size() - 2);
                    String cacheName = cacheAddress.getLastElement().getValue();
                    String containerName = containerAddress.getLastElement().getValue();
                    ServiceName cacheConfigurationServiceName = CacheConfigurationService.getServiceName(containerName, cacheName);

                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(cacheConfigurationServiceName);
                    if(controller != null) {
                        context.reloadRequired();
                    }
                     context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}

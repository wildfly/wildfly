package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.BaseJDBCStoreResource.COMMON_JDBC_STORE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.BaseStoreResource.COMMON_STORE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.EvictionResource.EVICTION_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.ExpirationResource.EXPIRATION_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResource.LOCKING_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.StateTransferResource.STATE_TRANSFER_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResource.WRITE_BEHIND_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResource.TRANSACTION_ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;
import java.util.Arrays;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Common code for handling the following cache configuration elements
 * {locking, transaction, eviction, expiration, state-transfer, rehashing, store, file-store, jdbc-store, remote-store}
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigOperationHandlers {

    /** The cache locking config add operation handler. */
    static final OperationStepHandler LOCKING_ADD = new BasicCacheConfigAdd(LOCKING_ATTRIBUTES);

    /** The cache transaction config add operation handler. */
    static final OperationStepHandler TRANSACTION_ADD = new BasicCacheConfigAdd(TRANSACTION_ATTRIBUTES);

    /** The cache eviction config add operation handler. */
    static final OperationStepHandler EVICTION_ADD = new BasicCacheConfigAdd(EVICTION_ATTRIBUTES);

    /** The cache expiration config add operation handler. */
    static final OperationStepHandler EXPIRATION_ADD = new BasicCacheConfigAdd(EXPIRATION_ATTRIBUTES);

    /** The cache state transfer config add operation handler. */
    static final OperationStepHandler STATE_TRANSFER_ADD = new BasicCacheConfigAdd(STATE_TRANSFER_ATTRIBUTES);

    /** The cache store config add operation handler. */
    static final OperationStepHandler STORE_ADD = new CacheStoreAdd();

    /** The cache store write-behind config add operation handler. */
    static final OperationStepHandler STORE_WRITE_BEHIND_ADD = new BasicCacheConfigAdd(WRITE_BEHIND_ATTRIBUTES);

    /** The cache file-store config add operation handler. */
    static final OperationStepHandler FILE_STORE_ADD = new FileCacheStoreAdd();

    /** The cache string-keyed-jdbc-store config add operation handler. */
    static final OperationStepHandler STRING_KEYED_JDBC_STORE_ADD = new StringKeyedJDBCCacheStoreAdd();

    /** The cache binary-keyed-jdbc-store config add operation handler. */
    static final OperationStepHandler BINARY_KEYED_JDBC_STORE_ADD = new BinaryKeyedJDBCCacheStoreAdd();

    /** The cache mixed-keyed-jdbc-store config add operation handler. */
    static final OperationStepHandler MIXED_KEYED_JDBC_STORE_ADD = new MixedKeyedJDBCCacheStoreAdd();

    /** The cache remote-store config add operation handler. */
    static final OperationStepHandler REMOTE_STORE_ADD = new RemoteCacheStoreAdd();

    /** The cache config remove operation handler. */
    static final OperationStepHandler REMOVE = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
            reloadRequiredStep(context);
            context.stepCompleted();
        }
    };

    /** The cache config property add operation handler. */
    static final OperationStepHandler STORE_PROPERTY_ADD = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            StorePropertyResource.VALUE.validateAndSet(operation, resource.getModel());
            reloadRequiredStep(context);
            context.stepCompleted();
        }
    };

    static final OperationStepHandler STORE_PROPERTY_ATTR = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            StorePropertyResource.VALUE.validateAndSet(operation, resource.getModel());
            reloadRequiredStep(context);
            context.stepCompleted();
        }
    };

    /**
     * Helper class to process adding basic nested cache configuration elements to the cache parent resource.
     * Override the process method in order to process configuration specific elements.
     *
     */
    private static class BasicCacheConfigAdd implements OperationStepHandler  {
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

            // Process type specific properties if required
            process(subModel, operation);

            // This needs a reload
            reloadRequiredStep(context);
            context.stepCompleted();
        }

        void process(ModelNode subModel, ModelNode operation) {
            //
        };
    }

    /**
     * Base class for adding cache stores.
     *
     * This class needs to do the following:
     * - check that its parent has no existing defined cache store
     * - process its model attributes
     * - create any child resources required for the store resource, such as a
     * set of properties
     *
     */
    abstract static class AbstractCacheStoreAdd extends AbstractAddStepHandler {
        private final AttributeDefinition[] attributes;

        AbstractCacheStoreAdd() {
            this.attributes = BaseStoreResource.COMMON_STORE_PARAMETERS;
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            final ModelNode model = resource.getModel();

            // need to check that the parent does not contain some other cache store ModelNode
            if (isCacheStoreDefined(context, operation)) {
                String storeName = getDefinedCacheStore(context, operation);
                throw new OperationFailedException(new ModelNode().set("cache store " + storeName + " is already defined"));
            }

            // Process attributes
            for(final AttributeDefinition attribute : attributes) {
                // we use PROPERTIES only to allow the user to pass in a list of properties on store add commands
                // don't copy these into the model
                if (attribute.getName().equals(BaseStoreResource.PROPERTIES.getName()))
                    continue ;
                attribute.validateAndSet(operation, model);
            }

            // Process type specific properties if required
            populateSubclassModel(context, operation, model);

            // The cache config parameters  <property name=>value</property>
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
        }

        abstract void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException ;

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            // do nothing
        }
    }

    /**
     * Add a basic cache store to a cache.
     */
    private static class CacheStoreAdd extends AbstractCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        CacheStoreAdd() {
            this.attributes = StoreResource.STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
        }
    }

    /**
     * Add a file cache store to a cache.
     */
    private static class FileCacheStoreAdd extends AbstractCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        FileCacheStoreAdd() {
            this.attributes = FileStoreResource.FILE_STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
        }
    }

    private static class JDBCCacheStoreAdd extends AbstractCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        JDBCCacheStoreAdd() {
            this.attributes = BaseJDBCStoreResource.COMMON_JDBC_STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
        }
    }

    private static class StringKeyedJDBCCacheStoreAdd extends JDBCCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        StringKeyedJDBCCacheStoreAdd() {
            this.attributes = StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            super.populateSubclassModel(context, operation, model);

            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
            // now check for string-keyed-table passed as optional parameter, in order to create the resource
//            if (operation.get("string-keyed-table").isDefined()) {
//                ModelNode stringTable = operation.get("string-keyed-table") ;
//                // process this table DMR description
//            }
        }
    }

    private static class BinaryKeyedJDBCCacheStoreAdd extends JDBCCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        BinaryKeyedJDBCCacheStoreAdd() {
            this.attributes = BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            super.populateSubclassModel(context, operation, model);

            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
            // now check for binary-keyed-table passed as optional parameter in order to create the resource
//            if (operation.get("binary-keyed-table").isDefined()) {
//                ModelNode binaryTable = operation.get("binary-keyed-table") ;
//                // process this table DMR description
//            }
        }
    }

    private static class MixedKeyedJDBCCacheStoreAdd extends JDBCCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        MixedKeyedJDBCCacheStoreAdd() {
            this.attributes = MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            super.populateSubclassModel(context, operation, model);

            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
            // now check for string-keyed and binary-keyed-table passed as optional parameter
            // now check for string-keyed-table passed as optional parameter
        }
    }

    private static class RemoteCacheStoreAdd extends AbstractCacheStoreAdd {
        private final AttributeDefinition[] attributes;

        RemoteCacheStoreAdd() {
            this.attributes = RemoteStoreResource.REMOTE_STORE_ATTRIBUTES;
        }

        @Override
        protected void populateSubclassModel(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            // this abstract method is called when populateModel() is called in the base class
            for(final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
            // now check for outbound connections passed as optional parameter
        }
    }



    static ModelNode createOperation(AttributeDefinition[] attributes, ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    static ModelNode createStoreOperation(AttributeDefinition[] commonAttributes, ModelNode address, ModelNode existing, AttributeDefinition... additionalAttributes) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition attribute : commonAttributes) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : additionalAttributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    static ModelNode createStringKeyedStoreOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition attribute : COMMON_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : COMMON_JDBC_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : STRING_KEYED_JDBC_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    static ModelNode createBinaryKeyedStoreOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition attribute : COMMON_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : COMMON_JDBC_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : BINARY_KEYED_JDBC_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    static ModelNode createMixedKeyedStoreOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition attribute : COMMON_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : COMMON_JDBC_STORE_ATTRIBUTES) {
            attribute.validateAndSet(existing, operation);
        }
        for(final AttributeDefinition attribute : MIXED_KEYED_JDBC_STORE_ATTRIBUTES) {
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
        if (context.getProcessType().isServer() && !context.isBooting()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                    // shorten this by providing static getServiceName methods which take an OP_ADDR
//                    PathAddress elementAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
//                    PathAddress cacheAddress = elementAddress.subAddress(0, elementAddress.size() - 1);
//                    PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size() - 2);
//                    String cacheName = cacheAddress.getLastElement().getValue();
//                    String containerName = containerAddress.getLastElement().getValue();
//                    ServiceName cacheConfigurationServiceName = CacheConfigurationService.getServiceName(containerName, cacheName);

//                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(cacheConfigurationServiceName);
//                    if(controller != null) {
//                        context.reloadRequired();
//                    }
                    context.reloadRequired();
                    context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    private static PathAddress getCacheAddress(ModelNode operation) {
        PathAddress cacheStoreAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathAddress cacheAddress = cacheStoreAddress.subAddress(0, cacheStoreAddress.size()-1);
        return cacheAddress;
    }

    private static ModelNode getCache(OperationContext context, PathAddress cacheAddress) {
        //Resource rootResource = context.readResourceFromRoot(cacheAddress, true);
        //ModelNode cache = rootResource.getModel();
        ModelNode cache = Resource.Tools.readModel(context.readResourceFromRoot(cacheAddress));
        return cache ;
    }
/*
    private static Resource getCacheResource(OperationContext context, PathAddress cacheAddress) {
        Resource rootResource = context.readResourceFromRoot(cacheAddress, true);
        return rootResource ;
    }
*/
    private static boolean isCacheStoreDefined(OperationContext context, ModelNode operation) {
         ModelNode cache = getCache(context, getCacheAddress(operation)) ;

         return (hasCustomStore(cache) || hasFileStore(cache) ||
                 hasStringKeyedJdbcStore(cache) || hasBinaryKeyedJdbcStore(cache) || hasMixedKeyedJdbcStore(cache) ||
                 hasRemoteStore(cache)) ;
    }

    private static String getDefinedCacheStore(OperationContext context, ModelNode operation) {
        ModelNode cache = getCache(context, getCacheAddress(operation)) ;
        if (hasCustomStore(cache))
            return ModelKeys.STORE ;
        else if (hasFileStore(cache))
            return ModelKeys.FILE_STORE ;
        else if (hasStringKeyedJdbcStore(cache))
            return ModelKeys.STRING_KEYED_JDBC_STORE ;
        else if (hasBinaryKeyedJdbcStore(cache))
            return ModelKeys.BINARY_KEYED_JDBC_STORE ;
        else if (hasMixedKeyedJdbcStore(cache))
            return ModelKeys.MIXED_KEYED_JDBC_STORE ;
        else if (hasRemoteStore(cache))
            return ModelKeys.REMOTE_STORE ;
        else
            return null ;
    }

    private static boolean hasCustomStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.STORE) && cache.get(ModelKeys.STORE, ModelKeys.STORE_NAME).isDefined() ;
    }

    private static boolean hasFileStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.FILE_STORE) && cache.get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME).isDefined() ;
    }

    private static boolean hasStringKeyedJdbcStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.STRING_KEYED_JDBC_STORE) && cache.get(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME).isDefined() ;
    }

    private static boolean hasBinaryKeyedJdbcStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.BINARY_KEYED_JDBC_STORE) && cache.get(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME).isDefined() ;
    }

    private static boolean hasMixedKeyedJdbcStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.MIXED_KEYED_JDBC_STORE) && cache.get(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME).isDefined() ;
    }

    private static boolean hasRemoteStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.REMOTE_STORE) && cache.get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME).isDefined() ;
    }

    // join two arrays
    private static AttributeDefinition[] combine(AttributeDefinition[] one, AttributeDefinition[] two) {

        ArrayList<AttributeDefinition> list = new ArrayList<AttributeDefinition>(Arrays.asList(one)) ;
        list.addAll(Arrays.asList(two));
        AttributeDefinition[] allValueTypes = new AttributeDefinition[list.size()];
        list.toArray(allValueTypes);
        return allValueTypes;
    }
}

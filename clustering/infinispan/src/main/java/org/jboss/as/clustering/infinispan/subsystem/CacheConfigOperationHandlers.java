package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.EvictionResource.EVICTION_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.ExpirationResource.EXPIRATION_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResource.LOCKING_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.StateTransferResource.STATE_TRANSFER_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResource.WRITE_BEHIND_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResource.TRANSACTION_ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;

/**
 * Common code for handling the following cache configuration elements
 * {locking, transaction, eviction, expiration, state-transfer, rehashing, store, file-store, jdbc-store, remote-store}
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigOperationHandlers {

    static final OperationStepHandler LOCKING_ADD = new CacheConfigAdd(LOCKING_ATTRIBUTES);
    static final OperationStepHandler TRANSACTION_ADD = new CacheConfigAdd(TRANSACTION_ATTRIBUTES);
    static final OperationStepHandler EVICTION_ADD = new CacheConfigAdd(EVICTION_ATTRIBUTES);
    static final OperationStepHandler EXPIRATION_ADD = new CacheConfigAdd(EXPIRATION_ATTRIBUTES);
    static final OperationStepHandler STATE_TRANSFER_ADD = new CacheConfigAdd(STATE_TRANSFER_ATTRIBUTES);

    static final OperationStepHandler STORE_ADD = new CacheStoreAdd();
    static final OperationStepHandler STORE_WRITE_BEHIND_ADD = new CacheConfigAdd(WRITE_BEHIND_ATTRIBUTES);
    static final OperationStepHandler STORE_PROPERTY_ADD = new CacheConfigAdd(new AttributeDefinition[]{StorePropertyResource.VALUE});
    static final OperationStepHandler FILE_STORE_ADD = new FileCacheStoreAdd();
    static final OperationStepHandler STRING_KEYED_JDBC_STORE_ADD = new StringKeyedJDBCCacheStoreAdd();
    static final OperationStepHandler BINARY_KEYED_JDBC_STORE_ADD = new BinaryKeyedJDBCCacheStoreAdd();
    static final OperationStepHandler MIXED_KEYED_JDBC_STORE_ADD = new MixedKeyedJDBCCacheStoreAdd();
    static final OperationStepHandler REMOTE_STORE_ADD = new RemoteCacheStoreAdd();

    /**
     * Helper class to process adding basic nested cache configuration elements to the cache parent resource.
     * When additional configuration is added, services need to be restarted; we restart all of them, for now
     * by indicating reload required.
     */
    private static class CacheConfigAdd extends AbstractAddStepHandler  {
        private final AttributeDefinition[] attributes;

        CacheConfigAdd(final AttributeDefinition[] attributes) {
            this.attributes = attributes;
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attr : attributes) {
                attr.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
            super.performRuntime(context, operation, model, verificationHandler, newControllers);
            // once we add a cache configuration, we need to restart all the services for the changes to take effect
            context.reloadRequired();
        }
     }

    /**
     * Base class for adding cache stores.
     *
     * This class needs to do the following:
     * - check that its parent has no existing defined cache store
     * - process its model attributes
     * - create any child resources required for the store resource, such as a set of properties
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
                throw InfinispanMessages.MESSAGES.cacheStoreAlreadyDefined(storeName);
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
                        throw InfinispanMessages.MESSAGES.propertyValueNotDefined(property.getName());
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
}

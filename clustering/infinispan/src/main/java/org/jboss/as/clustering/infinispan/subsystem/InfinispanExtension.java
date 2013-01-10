/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.DiscardAttributesTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Defines the Infinispan subsystem and its addressable resources.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "infinispan";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    public static final String RESOURCE_NAME = InfinispanExtension.class.getPackage().getName() + "." +"LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 4;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
           StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
           for (String kp : keyPrefix) {
               prefix.append('.').append(kp);
           }
            return new InfinispanResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, InfinispanExtension.class.getClassLoader());
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        // IMPORTANT: Management API version != xsd version! Not all Management API changes result in XSD changes
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        // Create the path resolver handler
        final ResolvePathHandler resolvePathHandler;
        if (context.getProcessType().isServer()) {
            resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                    .setPathAttribute(FileStoreResource.PATH)
                    .setRelativeToAttribute(FileStoreResource.RELATIVE_TO)
                    .build();
        } else {
            resolvePathHandler = null;
        }

        subsystem.registerSubsystemModel(new InfinispanSubsystemRootResource(resolvePathHandler));
        subsystem.registerXMLElementWriter(new InfinispanSubsystemXMLWriter());
        registerTransformers(subsystem);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }

    /**
     * Register the transformers for transforming from 1.4.0 to 1.3.0 management api versions, in which:
     * - attributes INDEXING_PROPERTIES, SEGMENTS were added in 1.4
     * - attribute VIRTUAL_NODES was deprecated in 1.4
     * - expression support was added to most attributes in 1.4, except for CLUSTER, DEFAULT_CACHE and MODE
     * for which it was already enabled in 1.3
     *
     * Chaining of transformers is used in cases where two transformers are required for the same operation.
     *
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers(final SubsystemRegistration subsystem) {

        // define the resource and operation transformers

        // Register the model transformers
        TransformersSubRegistration registration = subsystem.registerModelTransformers(ModelVersion.create(1, 3), ResourceTransformer.DEFAULT);

        final RejectExpressionValuesTransformer cacheContainerReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_CONTAINER_ATTRIBUTES);
        TransformersSubRegistration containerRegistration =
                registerTransformer(registration, CacheContainerResource.CONTAINER_PATH, cacheContainerReject, cacheContainerReject, cacheContainerReject.getWriteAttributeTransformer(), null);

        final RejectExpressionValuesTransformer transportReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_TRANSPORT_ATTRIBUTES);
        registerTransformer(containerRegistration, TransportResource.TRANSPORT_PATH, transportReject, transportReject, transportReject.getWriteAttributeTransformer(), null);

        final InfinispanDiscardAttributesTransformer removeSelectedCacheAttributes = new InfinispanDiscardAttributesTransformer(ModelKeys.INDEXING_PROPERTIES, ModelKeys.SEGMENTS, ModelKeys.VIRTUAL_NODES);
        final RejectExpressionValuesTransformer cacheReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_CACHE_ATTRIBUTES);
        final ChainedResourceTransformer chainedResource = new ChainedResourceTransformer(removeSelectedCacheAttributes, cacheReject.getChainedTransformer());
        final ChainedOperationTransformer chainedAdd = new ChainedOperationTransformer(removeSelectedCacheAttributes, cacheReject);
        final ChainedOperationTransformer chainedWrite = new ChainedOperationTransformer(removeSelectedCacheAttributes.getWriteAttributeTransformer(), cacheReject.getWriteAttributeTransformer());


        PathElement[] cachePaths = {
                LocalCacheResource.LOCAL_CACHE_PATH,
                InvalidationCacheResource.INVALIDATION_CACHE_PATH,
                ReplicatedCacheResource.REPLICATED_CACHE_PATH,
                DistributedCacheResource.DISTRIBUTED_CACHE_PATH};
        for (int i=0; i < cachePaths.length; i++) {
            // register chained operation transformers for cache ADD operations where we need to remove and reject
            TransformersSubRegistration cacheRegistration = //containerRegistration.registerSubResource(cachePaths[i], (OperationTransformer) chainedAdd);
                    registerTransformer(containerRegistration, cachePaths[i], chainedResource, chainedAdd, chainedWrite, removeSelectedCacheAttributes.getUndefineAttributeTransformer());

            registerCacheChildrenTransformers(cacheRegistration) ;
        }
    }

    private static TransformersSubRegistration registerTransformer(TransformersSubRegistration parent, PathElement path, ResourceTransformer resourceTransformer, OperationTransformer addTransformer,
                        OperationTransformer writeAttributeTransformer, OperationTransformer undefineAttributeTransformer) {
        TransformersSubRegistration childReg = parent.registerSubResource(path, resourceTransformer);
        childReg.registerOperationTransformer(ADD, addTransformer);
        childReg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, writeAttributeTransformer);
        if (undefineAttributeTransformer != null) {
            childReg.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, undefineAttributeTransformer);
        }
        return childReg;
    }

    private static void registerCacheChildrenTransformers(TransformersSubRegistration cacheReg) {

        final RejectExpressionValuesTransformer childReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_CHILD_ATTRIBUTES) ;

        PathElement[] childPaths = {
                LockingResource.LOCKING_PATH,
                TransactionResource.TRANSACTION_PATH,
                ExpirationResource.EXPIRATION_PATH,
                EvictionResource.EVICTION_PATH,
                StateTransferResource.STATE_TRANSFER_PATH
        } ;

        for (int i=0; i < childPaths.length; i++) {
            // reject expressions on operations in children
            cacheReg.registerSubResource(childPaths[i], (OperationTransformer) childReject);
        }

        final RejectExpressionValuesTransformer storeReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_STORE_ATTRIBUTES);
        PathElement[] storePaths = {
                StoreResource.STORE_PATH,
                FileStoreResource.FILE_STORE_PATH,
                StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_PATH,
                BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_PATH,
                MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_PATH,
                RemoteStoreResource.REMOTE_STORE_PATH
        } ;

        for (int i=0; i < storePaths.length; i++) {
            // reject expressions on operations on stores and store properties
            TransformersSubRegistration storeReg = cacheReg.registerSubResource(storePaths[i], (OperationTransformer) storeReject);
            storeReg.registerSubResource(StoreWriteBehindResource.STORE_WRITE_BEHIND_PATH, (OperationTransformer) storeReject);
            storeReg.registerSubResource(StorePropertyResource.STORE_PROPERTY_PATH, (OperationTransformer) storeReject);
        }
    }

    private static class InfinispanResourceDescriptionResolver extends StandardResourceDescriptionResolver {

        // a static map, mapping attribute names to the prefixes that should be used to look up their descriptions
        private static final Map<String, String> sharedAttributeResolver = new HashMap<String, String>() ;

        static {
            // shared cache attributes
            sharedAttributeResolver.put(CacheResource.BATCHING.getName(), "cache");
            sharedAttributeResolver.put(CacheResource.CACHE_MODULE.getName(), "cache");
            sharedAttributeResolver.put(CacheResource.INDEXING.getName(), "cache");
            sharedAttributeResolver.put(CacheResource.INDEXING_PROPERTIES.getName(), "cache");
            sharedAttributeResolver.put(CacheResource.JNDI_NAME.getName(), "cache");
            sharedAttributeResolver.put(CacheResource.NAME.getName(), "cache");
            sharedAttributeResolver.put(CacheResource.START.getName(), "cache");

            sharedAttributeResolver.put(ClusteredCacheResource.ASYNC_MARSHALLING.getName(), "clustered-cache");
            sharedAttributeResolver.put(ClusteredCacheResource.MODE.getName(), "clustered-cache");
            sharedAttributeResolver.put(ClusteredCacheResource.QUEUE_FLUSH_INTERVAL.getName(), "clustered-cache");
            sharedAttributeResolver.put(ClusteredCacheResource.QUEUE_SIZE.getName(), "clustered-cache");
            sharedAttributeResolver.put(ClusteredCacheResource.REMOTE_TIMEOUT.getName(), "clustered-cache");

            sharedAttributeResolver.put(BaseStoreResource.FETCH_STATE.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.PASSIVATION.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.PRELOAD.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.PURGE.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.SHARED.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.SINGLETON.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.PROPERTY.getName(), "store");
            sharedAttributeResolver.put(BaseStoreResource.PROPERTIES.getName(), "store");

            sharedAttributeResolver.put(BaseJDBCStoreResource.DATA_SOURCE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.BATCH_SIZE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.FETCH_SIZE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.PREFIX.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.ID_COLUMN.getName()+".column", "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.DATA_COLUMN.getName()+".column", "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.TIMESTAMP_COLUMN.getName()+".column", "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.ENTRY_TABLE.getName()+"table", "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.BUCKET_TABLE.getName()+"table", "jdbc-store");

            // shared children - this avoids having to describe the children for each parent resource
            sharedAttributeResolver.put(ModelKeys.TRANSPORT, null);
            sharedAttributeResolver.put(ModelKeys.LOCKING, null);
            sharedAttributeResolver.put(ModelKeys.TRANSACTION, null);
            sharedAttributeResolver.put(ModelKeys.EVICTION, null);
            sharedAttributeResolver.put(ModelKeys.EXPIRATION, null);
            sharedAttributeResolver.put(ModelKeys.STATE_TRANSFER, null);
            sharedAttributeResolver.put(ModelKeys.STORE, null);
            sharedAttributeResolver.put(ModelKeys.FILE_STORE, null);
            sharedAttributeResolver.put(ModelKeys.REMOTE_STORE, null);
            sharedAttributeResolver.put(ModelKeys.STRING_KEYED_JDBC_STORE, null);
            sharedAttributeResolver.put(ModelKeys.BINARY_KEYED_JDBC_STORE, null);
            sharedAttributeResolver.put(ModelKeys.MIXED_KEYED_JDBC_STORE, null);
            sharedAttributeResolver.put(ModelKeys.WRITE_BEHIND, null);
            sharedAttributeResolver.put(ModelKeys.PROPERTY, null);
        }

        private InfinispanResourceDescriptionResolver(String keyPrefix, String bundleBaseName, ClassLoader bundleLoader) {
            super(keyPrefix, bundleBaseName, bundleLoader, true, false);
        }

        @Override
        public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(attributeName)) {
               return bundle.getString(getBundleKey(attributeName));
            }
            return super.getResourceAttributeDescription(attributeName, locale, bundle);
        }

        @Override
        public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(attributeName)) {
               return bundle.getString(getVariableBundleKey(attributeName, suffixes));
            }
            return super.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(paramName)) {
               return bundle.getString(getBundleKey(paramName));
            }
            return super.getOperationParameterDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(paramName)) {
               return bundle.getString(getVariableBundleKey(paramName, suffixes));
            }
            return super.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
        }

        @Override
        public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(childType)) {
               return bundle.getString(getBundleKey(childType));
            }
            return super.getChildTypeDescription(childType, locale, bundle);
        }

        private String getBundleKey(final String name) {
            return getVariableBundleKey(name);
        }

        private String getVariableBundleKey(final String name, final String... variable) {
            final String prefix = sharedAttributeResolver.get(name);
            StringBuilder sb = new StringBuilder(SUBSYSTEM_NAME);
            // construct the key prefix
            if (prefix == null) {
                sb = sb.append('.').append(name);
            } else {
                sb = sb.append('.').append(prefix).append('.').append(name);
            }
            // construct the key suffix
            if (variable != null) {
                for (String arg : variable) {
                    if (sb.length() > 0)
                        sb.append('.');
                    sb.append(arg);
                }
            }
            return sb.toString();
        }
    }

    private static class InfinispanDiscardAttributesTransformer extends DiscardAttributesTransformer {
        InfinispanDiscardAttributesTransformer(String...attributes){
            super(attributes);
        }
    }
}

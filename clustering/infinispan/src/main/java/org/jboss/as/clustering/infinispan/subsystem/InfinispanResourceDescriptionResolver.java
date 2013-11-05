package org.jboss.as.clustering.infinispan.subsystem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * Custom resource description resolver to handle resources structured in a class hierarchy
 * which need to share resource name definitions.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanResourceDescriptionResolver extends StandardResourceDescriptionResolver {

    private Map<String, String> sharedAttributeResolver = new HashMap<String, String>();

    public InfinispanResourceDescriptionResolver(String keyPrefix, String bundleBaseName, ClassLoader bundleLoader) {
        super(keyPrefix, bundleBaseName, bundleLoader, true, false);
        initMap();
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
        StringBuilder sb = new StringBuilder(InfinispanExtension.SUBSYSTEM_NAME);
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

    private void initMap() {
        // shared cache attributes
        sharedAttributeResolver.put(CacheResourceDefinition.BATCHING.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.CACHE_MODULE.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.INDEXING.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.INDEXING_PROPERTIES.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.JNDI_NAME.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.NAME.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.START.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.STATISTICS.getName(), "cache");

        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.MODE.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.QUEUE_SIZE.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.getName(), "clustered-cache");

        sharedAttributeResolver.put(BaseStoreResourceDefinition.FETCH_STATE.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.PASSIVATION.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.PRELOAD.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.PURGE.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.SHARED.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.SINGLETON.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.PROPERTY.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.PROPERTIES.getName(), "store");

        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.DATA_SOURCE.getName(), "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.BATCH_SIZE.getName(), "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.FETCH_SIZE.getName(), "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.PREFIX.getName(), "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.ID_COLUMN.getName() + ".column", "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.DATA_COLUMN.getName() + ".column", "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.TIMESTAMP_COLUMN.getName() + ".column", "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.ENTRY_TABLE.getName() + "table", "jdbc-store");
        sharedAttributeResolver.put(BaseJDBCStoreResourceDefinition.BUCKET_TABLE.getName() + "table", "jdbc-store");

        // shared cache metrics
        sharedAttributeResolver.put(CacheResourceDefinition.ACTIVATIONS.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.AVERAGE_READ_TIME.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.AVERAGE_WRITE_TIME.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.CACHE_STATUS.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.ELAPSED_TIME.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.HIT_RATIO.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.HITS.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.INVALIDATIONS.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.MISSES.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.NUMBER_OF_ENTRIES.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.PASSIVATIONS.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.READ_WRITE_RATIO.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.REMOVE_HITS.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.REMOVE_MISSES.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.STORES.getName(), "cache");
        sharedAttributeResolver.put(CacheResourceDefinition.TIME_SINCE_RESET.getName(), "cache");

        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.AVERAGE_REPLICATION_TIME.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.REPLICATION_COUNT.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.REPLICATION_FAILURES.getName(), "clustered-cache");
        sharedAttributeResolver.put(ClusteredCacheResourceDefinition.SUCCESS_RATIO.getName(), "clustered-cache");

        sharedAttributeResolver.put(BaseStoreResourceDefinition.CACHE_LOADER_LOADS.getName(), "store");
        sharedAttributeResolver.put(BaseStoreResourceDefinition.CACHE_LOADER_MISSES.getName(), "store");

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
        sharedAttributeResolver.put(ModelKeys.BACKUP_FOR, null);
    }
}

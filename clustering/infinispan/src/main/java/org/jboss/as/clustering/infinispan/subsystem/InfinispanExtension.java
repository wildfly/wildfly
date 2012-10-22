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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
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
    public static final String RESOURCE_NAME = InfinispanExtension.class.getPackage().getName() + "." +"LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 4;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;


    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
           StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
           for (String kp : keyPrefix) {
               prefix.append('.').append(kp);
           }
           //return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, InfinispanExtension.class.getClassLoader(), true, false);
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

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new InfinispanSubsystemRootResource());
        registration.registerOperationHandler(DESCRIBE, InfinispanSubsystemDescribe.INSTANCE, InfinispanSubsystemDescribe.SUBSYSTEM_DESCRIBE, false, EntryType.PRIVATE);

        ManagementResourceRegistration cacheContainer = registration.registerSubModel(new CacheContainerResource());

        subsystem.registerXMLElementWriter(new InfinispanSubsystemXMLWriter());

        /*
        // Register the model transformers
        TransformersSubRegistration reg = subsystem.registerModelTransformers(ModelVersion.create(1, 3), new InfinispanSubsystemTransformer_1_3());
        TransformersSubRegistration containerReg = reg.registerSubResource(CacheContainerResource.CONTAINER_PATH);
        InfinispanOperationTransformer_1_3 ot = new InfinispanOperationTransformer_1_3();
        containerReg.registerSubResource(LocalCacheResource.LOCAL_CACHE_PATH).registerOperationTransformer(ADD, ot);
        containerReg.registerSubResource(InvalidationCacheResource.INVALIDATION_CACHE_PATH).registerOperationTransformer(ADD, ot);
        containerReg.registerSubResource(ReplicatedCacheResource.REPLICATED_CACHE_PATH).registerOperationTransformer(ADD, ot);
        containerReg.registerSubResource(DistributedCacheResource.DISTRIBUTED_CACHE_PATH).registerOperationTransformer(ADD, ot);
        */
    }

    private static class InfinispanOperationTransformer_1_3 extends AbstractOperationTransformer {
        @Override
        protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
            if (operation.has(ModelKeys.INDEXING_PROPERTIES)){
                operation.remove(ModelKeys.INDEXING_PROPERTIES);
            }
            return operation;
        }
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


    private static class InfinispanResourceDescriptionResolver extends StandardResourceDescriptionResolver {

        // a static map, mapping attribute names to the prefixes that should be used to look up their descriptions
        private static final Map<String, String> sharedAttributeResolver = new HashMap<String, String>() ;

        static {
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
            sharedAttributeResolver.put(BaseJDBCStoreResource.COLUMN_NAME.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.COLUMN_TYPE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.ID_COLUMN.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.DATA_COLUMN.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.TIMESTAMP_COLUMN.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.ENTRY_TABLE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.BUCKET_TABLE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.STRING_KEYED_TABLE.getName(), "jdbc-store");
            sharedAttributeResolver.put(BaseJDBCStoreResource.BINARY_KEYED_TABLE.getName(), "jdbc-store");
        }

        private InfinispanResourceDescriptionResolver(String keyPrefix, String bundleBaseName, ClassLoader bundleLoader) {
            super(keyPrefix, bundleBaseName, bundleLoader);

//            System.out.println("dumping InfinispanResourceDescriptionResolver attribute table:");
//            for (Map.Entry entry : sharedAttributeResolver.entrySet()) {
//                System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
//            }
        }

        @Override
        public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
            System.out.println("resource attribute description: name = " + attributeName);
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(attributeName)) {
               return bundle.getString(getBundleKey(attributeName));
            }
            return super.getResourceAttributeDescription(attributeName, locale, bundle);
        }

        @Override
        public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
            System.out.println("resource attribute value type description: name = " + attributeName + " suffixes = " + suffixes);
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(attributeName)) {
               return bundle.getString(getVariableBundleKey(attributeName, suffixes));
            }
            return super.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
            System.out.println("operation parameter description: operation = " + operationName + ", parameter = " + paramName);
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(paramName)) {
               return bundle.getString(getBundleKey(paramName));
            }
            return super.getOperationParameterDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
            System.out.println("operation parameter value type description: operation = " + operationName + ", parameter = " + paramName + ", suffixes = " + suffixes);
            // don't apply the default bundle prefix to these attributes
            if (sharedAttributeResolver.containsKey(paramName)) {
               return bundle.getString(getVariableBundleKey(paramName, suffixes));
            }
            return super.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
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

}

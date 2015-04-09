/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Custom resource description resolver to handle resources structured in a class hierarchy
 * which need to share resource name definitions.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanResourceDescriptionResolver extends SubsystemResourceDescriptionResolver {

    private Map<String, String> sharedAttributeResolver = new HashMap<>();

    InfinispanResourceDescriptionResolver() {
        this(Collections.<String>emptyList());
    }

    InfinispanResourceDescriptionResolver(String keyPrefix) {
        this(Collections.singletonList(keyPrefix));
    }

    InfinispanResourceDescriptionResolver(String... keyPrefixes) {
        this(Arrays.asList(keyPrefixes));
    }

    private InfinispanResourceDescriptionResolver(List<String> keyPrefixes) {
        super(InfinispanExtension.SUBSYSTEM_NAME, keyPrefixes, InfinispanExtension.class);
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
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        if (sharedAttributeResolver.containsKey(attributeName)) {
            return bundle.getString(getVariableBundleKey(attributeName, ModelDescriptionConstants.DEPRECATED));
        }
        return super.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
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
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        if (sharedAttributeResolver.containsKey(paramName)) {
            return bundle.getString(getVariableBundleKey(paramName, ModelDescriptionConstants.DEPRECATED));
        }
        return super.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
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
        if (prefix != null) {
            sb.append('.').append(prefix);
        }
        sb.append('.').append(name);
        // construct the key suffix
        if (variable != null) {
            for (String arg : variable) {
                sb.append('.').append(arg);
            }
        }
        return sb.toString();
    }

    private void initMap() {
        for (AttributeDefinition attribute: CacheResourceDefinition.ATTRIBUTES) {
            sharedAttributeResolver.put(attribute.getName(), "cache");
        }
        for (AttributeDefinition attribute: ClusteredCacheResourceDefinition.ATTRIBUTES) {
            sharedAttributeResolver.put(attribute.getName(), "clustered-cache");
        }
        for (AttributeDefinition attribute: StoreResourceDefinition.ATTRIBUTES) {
            sharedAttributeResolver.put(attribute.getName(), "store");
        }
        for (AttributeDefinition attribute: JDBCStoreResourceDefinition.ATTRIBUTES) {
            sharedAttributeResolver.put(attribute.getName(), "jdbc-store");
        }

        this.initMetric(CacheMetric.class, "cache");
        this.initMetric(ClusteredCacheMetric.class, "clustered-cache");
        this.initMetric(EvictionMetric.class, "eviction");
        this.initMetric(LockingMetric.class, "locking");
        this.initMetric(StoreMetric.class, "store");
        this.initMetric(TransactionMetric.class, "transaction");

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

    private <E extends Enum<E> & Metric<?>> void initMetric(Class<E> metricClass, String prefix) {
        for (Metric<?> metric: EnumSet.allOf(metricClass)) {
            this.sharedAttributeResolver.put(metric.getDefinition().getName(), prefix);
        }
    }
}

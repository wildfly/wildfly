/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import java.util.function.Predicate;

import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.util.IntSet;
import org.infinispan.persistence.jdbc.stringbased.JdbcStringBasedStore;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.reactivestreams.Publisher;

/**
 * Custom JDBC cache store implementation that executes all publisher actions on the caller thread.
 * @author Paul Ferraro
 */
@ConfiguredBy(JDBCStoreConfiguration.class)
public class JDBCStore<K, V> extends JdbcStringBasedStore<K, V> {

    @Override
    public Publisher<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean includeValues) {
        // Execute subscribe, observer, and finally actions on same thread
        return this.tableOperations.publishEntries(this.connectionFactory::getConnection, this.connectionFactory::releaseConnection, segments, filter, includeValues);
    }

    @Override
    public Publisher<K> publishKeys(IntSet segments, Predicate<? super K> filter) {
        // Execute subscribe, observer, and finally actions on same thread
        return this.tableOperations.publishKeys(this.connectionFactory::getConnection, this.connectionFactory::releaseConnection, segments, filter);
    }
}

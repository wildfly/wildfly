/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence.sifs;

import static org.infinispan.configuration.cache.AbstractStoreConfiguration.SEGMENTED;
import static org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration.COMPACTION_THRESHOLD;
import static org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration.OPEN_FILES_LIMIT;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalStateConfiguration;
import org.infinispan.persistence.PersistenceUtil;
import org.infinispan.persistence.sifs.Log;
import org.infinispan.persistence.sifs.NonBlockingSoftIndexFileStore;
import org.infinispan.persistence.sifs.configuration.DataConfiguration;
import org.infinispan.persistence.sifs.configuration.DataConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.IndexConfiguration;
import org.infinispan.persistence.sifs.configuration.IndexConfigurationBuilder;
import org.infinispan.util.logging.LogFactory;

/**
 * Workaround for ISPN-13605.
 * @author Paul Ferraro
 */
public class SoftIndexFileStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<SoftIndexFileStoreConfiguration, SoftIndexFileStoreConfigurationBuilder> {
    private static final Log LOG = LogFactory.getLog(SoftIndexFileStoreConfigurationBuilder.class, Log.class);

    protected final IndexConfigurationBuilder index = new IndexConfigurationBuilder();
    protected final DataConfigurationBuilder data = new DataConfigurationBuilder();

    public SoftIndexFileStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
        super(builder, org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration.attributeDefinitionSet(), AsyncStoreConfiguration.attributeDefinitionSet());
    }

    /**
     * The path where the Soft-Index store will keep its data files. Under this location the store will create
     * a directory named after the cache name, under which a <code>data</code> directory will be created.
     *
     * The default behaviour is to use the {@link GlobalStateConfiguration#persistentLocation()}.
     */
    public SoftIndexFileStoreConfigurationBuilder dataLocation(String dataLocation) {
        this.data.dataLocation(dataLocation);
        return this;
    }

    /**
     * The path where the Soft-Index store will keep its index files. Under this location the store will create
     * a directory named after the cache name, under which a <code>index</code> directory will be created.
     *
     * The default behaviour is to use the {@link GlobalStateConfiguration#persistentLocation()}.
     */
    public SoftIndexFileStoreConfigurationBuilder indexLocation(String indexLocation) {
        this.index.indexLocation(indexLocation);
        return this;
    }

    /**
     * Number of index segment files. Increasing this value improves throughput but requires more threads to be spawned.
     * <p>
     * Defaults to <code>16</code>.
     */
    public SoftIndexFileStoreConfigurationBuilder indexSegments(int indexSegments) {
        this.index.indexSegments(indexSegments);
        return this;
    }

    /**
     * Sets the maximum size of single data file with entries, in bytes.
     *
     * Defaults to <code>16777216</code> (16MB).
     */
    public SoftIndexFileStoreConfigurationBuilder maxFileSize(int maxFileSize) {
        this.data.maxFileSize(maxFileSize);
        return this;
    }

    /**
     * If the size of the node (continuous block on filesystem used in index implementation) drops below this threshold,
     * the node will try to balance its size with some neighbour node, possibly causing join of multiple nodes.
     *
     * Defaults to <code>0</code>.
     */
    public SoftIndexFileStoreConfigurationBuilder minNodeSize(int minNodeSize) {
        this.index.minNodeSize(minNodeSize);
        return this;
    }

    /**
     * Max size of node (continuous block on filesystem used in index implementation), in bytes.
     *
     * Defaults to <code>4096</code>.
     */
    public SoftIndexFileStoreConfigurationBuilder maxNodeSize(int maxNodeSize) {
        this.index.maxNodeSize(maxNodeSize);
        return this;
    }

    /**
     * Sets the maximum number of entry writes that are waiting to be written to the index, per index segment.
     *
     * Defaults to <code>1000</code>.
     */
    public SoftIndexFileStoreConfigurationBuilder indexQueueLength(int indexQueueLength) {
        this.index.indexQueueLength(indexQueueLength);
        return this;
    }

    /**
     * Sets whether writes shoud wait to be fsynced to disk.
     *
     * Defaults to <code>false</code>.
     */
    public SoftIndexFileStoreConfigurationBuilder syncWrites(boolean syncWrites) {
        this.data.syncWrites(syncWrites);
        return this;
    }

    /**
     * Sets the maximum number of open files.
     *
     * Defaults to <code>1000</code>.
     */
    public SoftIndexFileStoreConfigurationBuilder openFilesLimit(int openFilesLimit) {
        this.attributes.attribute(OPEN_FILES_LIMIT).set(openFilesLimit);
        return this;
    }

    /**
     * If the amount of unused space in some data file gets above this threshold, the file is compacted - entries from that file are copied to a new file and the old file is deleted.
     *
     * Defaults to <code>0.5</code> (50%).
     */
    public SoftIndexFileStoreConfigurationBuilder compactionThreshold(double compactionThreshold) {
        this.attributes.attribute(COMPACTION_THRESHOLD).set(compactionThreshold);
        return this;
    }

    @Override
    public SoftIndexFileStoreConfiguration create() {
        return new SoftIndexFileStoreConfiguration(this.attributes.protect(), this.async.create(), this.index.create(), this.data.create());
    }

    @Override
    public SoftIndexFileStoreConfigurationBuilder read(SoftIndexFileStoreConfiguration template) {
        super.read(template);
        this.index.read(template.index());
        this.data.read(template.data());
        return this;
    }

    @Override
    public SoftIndexFileStoreConfigurationBuilder self() {
        return this;
    }

    @Override
    protected void validate (boolean skipClassChecks) {
        Attribute<Boolean> segmentedAttribute = this.attributes.attribute(SEGMENTED);
        if (segmentedAttribute.isModified() && !segmentedAttribute.get()) {
            throw org.infinispan.util.logging.Log.CONFIG.storeRequiresBeingSegmented(NonBlockingSoftIndexFileStore.class.getSimpleName());
        }
        super.validate(skipClassChecks);
        this.index.validate();
        double compactionThreshold = this.attributes.attribute(COMPACTION_THRESHOLD).get();
        if (compactionThreshold <= 0 || compactionThreshold > 1) {
            throw LOG.invalidCompactionThreshold(compactionThreshold);
        }
    }

    @Override
    public void validate(GlobalConfiguration globalConfig) {
        PersistenceUtil.validateGlobalStateStoreLocation(globalConfig, NonBlockingSoftIndexFileStore.class.getSimpleName(),
                this.data.attributes().attribute(DataConfiguration.DATA_LOCATION),
                this.index.attributes().attribute(IndexConfiguration.INDEX_LOCATION));

        super.validate(globalConfig);
    }

    @Override
    public String toString () {
       return String.format("SoftIndexFileStoreConfigurationBuilder{index=%s, data=%s, attributes=%s, async=%s}", this.index, this.data, this.attributes, this.async);
    }
}

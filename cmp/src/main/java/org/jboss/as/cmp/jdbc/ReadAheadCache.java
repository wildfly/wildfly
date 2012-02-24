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
package org.jboss.as.cmp.jdbc;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.logging.Logger;
import org.jboss.tm.TransactionLocal;


/**
 * ReadAheadCache stores all of the data readahead for an entity.
 * Data is stored in the JDBCStoreManager entity tx data map on a per entity
 * basis. The read ahead data for each entity is stored with a soft reference.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class ReadAheadCache {
    /**
     * To simplify null values handling in the preloaded data pool we use
     * this value instead of 'null'
     */
    private static final Object NULL_VALUE = new Object();

    private final JDBCStoreManager manager;
    private final Logger log;

    private TransactionLocal listMapTxLocal;

    private ListCache listCache;
    private int listCacheMax;

    public ReadAheadCache(JDBCStoreManager manager) {
        this.manager = manager;

        // Create the Log
        log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());
    }

    public void create() {
        // Create the list cache
        listCacheMax = ((JDBCEntityBridge) manager.getEntityBridge()).getListCacheMax();
        listCache = new ListCache(listCacheMax);
    }

    public void start() {
        listMapTxLocal = new TransactionLocal(manager.getComponent().getTransactionManager()) {
            protected Object initialValue() {
                return new HashMap();
            }

            public Transaction getTransaction() {
                try {
                    return transactionManager.getTransaction();
                } catch (SystemException e) {
                    throw CmpMessages.MESSAGES.errorGettingCurrentTransaction(e);
                }
            }
        };
        listCache.start();
    }

    public void stop() {
        listCache.clear();
    }

    public void destroy() {
        listCache = null;
    }

    public void addFinderResults(List results, JDBCReadAheadMetaData readahead) {
        if (listCacheMax == 0 || results.size() < 2) {
            // nothing to see here... move along
            return;
        }

        Map listMap = getListMap();
        if (listMap == null) {
            // no active transaction
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Add finder results:" +
                    " entity=" + manager.getEntityBridge().getEntityName() +
                    " results=" + results +
                    " readahead=" + readahead);
        }

        // add the finder to the LRU list
        if (!readahead.isNone()) {
            listCache.add(results);
        }

        //
        // Create a map between the entity primary keys and the list.
        // The primary key will point to the last list added that contained the
        // primary key.
        //
        HashSet dereferencedResults = new HashSet();
        Iterator iter = results.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            Object pk = iter.next();

            // create the new entry object
            EntityMapEntry entry;
            if (readahead.isNone()) {
                entry = new EntityMapEntry(0, Collections.singletonList(pk), readahead);
            } else {
                entry = new EntityMapEntry(i, results, readahead);
            }

            // Keep track of the results that have been dereferenced. Later we
            // all results from the list cache that are no longer referenced.
            EntityMapEntry oldInfo = (EntityMapEntry) listMap.put(pk, entry);
            if (oldInfo != null) {
                dereferencedResults.add(oldInfo.results);
            }
        }

        //
        // Now we remove all lists from the list cache that are no longer
        // referenced in the list map.
        //

        // if we don't have any dereferenced results at this point we are done
        if (dereferencedResults.isEmpty()) {
            return;
        }

        //
        // Go through the dereferenced results set and look at the PKs for each
        // dereferenced list.  If you find one key that references the
        // dereferenced list, remove it from the dereferenced results set and
        // move on to the next dereferenced results.
        //
        iter = dereferencedResults.iterator();
        while (iter.hasNext()) {
            List dereferencedList = (List) iter.next();

            boolean listHasReference = false;
            Iterator iter2 = dereferencedList.iterator();
            while (!listHasReference &&
                    iter2.hasNext()) {
                EntityMapEntry entry = (EntityMapEntry) listMap.get(iter2.next());
                if (entry != null && entry.results == dereferencedList) {
                    listHasReference = true;
                }
            }

            if (listHasReference) {
                // this list does not have any references
                iter.remove();
            }
        }

        // if we don't have any dereferenced results at this point we are done
        if (dereferencedResults.isEmpty()) {
            return;
        }

        // remove all results from the cache that are no longer referenced
        iter = dereferencedResults.iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            if (log.isTraceEnabled()) {
                log.trace("Removing dereferenced results: " + list);
            }
            listCache.remove(list);
        }
    }

    private void removeFinderResult(List results) {
        Map listMap = getListMap();
        if (listMap == null) {
            // no active transaction
            return;
        }

        // remove the list from the list cache
        listCache.remove(results);

        // remove all primary keys from the listMap that reference this list
        if (!listMap.isEmpty()) {
            Iterator iter = listMap.values().iterator();
            while (iter.hasNext()) {
                EntityMapEntry entry = (EntityMapEntry) iter.next();

                // use == because only identity matters here
                if (entry.results == results) {
                    iter.remove();
                }
            }
        }
    }

    public EntityReadAheadInfo getEntityReadAheadInfo(Object pk) {
        Map listMap = getListMap();
        if (listMap == null) {
            // no active transaction
            return new EntityReadAheadInfo(Collections.singletonList(pk));
        }

        EntityMapEntry entry = (EntityMapEntry) getListMap().get(pk);
        if (entry != null) {
            // we're using these results so promote it to the head of the
            // LRU list
            if (!entry.readahead.isNone()) {
                listCache.promote(entry.results);
            }

            // get the readahead metadata
            JDBCReadAheadMetaData readahead = entry.readahead;
            if (readahead == null) {
                readahead = manager.getMetaData().getReadAhead();
            }

            int from = entry.index;
            int to = Math.min(entry.results.size(), entry.index + readahead.getPageSize());
            List loadKeys = entry.results.subList(from, to);
            return new EntityReadAheadInfo(loadKeys, readahead);
        } else {
            return new EntityReadAheadInfo(Collections.singletonList(pk));
        }
    }

    /**
     * Loads all of the preloaded data for the ctx into it.
     *
     * @param ctx the context that will be loaded
     * @return true if at least one field was loaded.
     */
    public boolean load(CmpEntityBeanContext ctx) {
        if (log.isTraceEnabled()) {
            log.trace("load data:" +
                    " entity=" + manager.getEntityBridge().getEntityName() +
                    " pk=" + ctx.getPrimaryKey());
        }

        // get the preload data map
        Map preloadDataMap = getPreloadDataMap(ctx.getPrimaryKey(), false);
        if (preloadDataMap == null || preloadDataMap.isEmpty()) {
            // no preloaded data for this entity
            if (log.isTraceEnabled()) {
                log.trace("No preload data found:" +
                        " entity=" + manager.getEntityBridge().getEntityName() +
                        " pk=" + ctx.getPrimaryKey());
            }
            return false;
        }

        boolean cleanReadAhead = manager.getMetaData().isCleanReadAheadOnLoad();

        boolean loaded = false;
        JDBCCMRFieldBridge onlyOneSingleValuedCMR = null;

        // iterate over the keys in the preloaded map
        Iterator iter = preloadDataMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object field = entry.getKey();

            // get the value that was preloaded for this field
            Object value = entry.getValue();

            // if we didn't get a value something is seriously hosed
            if (value == null) {
                throw MESSAGES.preloadedValueNotFound();
            }

            if (cleanReadAhead) {
                // remove this value from the preload cache as it is about to be loaded
                iter.remove();
            }

            // check for null value standin
            if (value == NULL_VALUE) {
                value = null;
            }

            if (field instanceof JDBCCMPFieldBridge) {
                JDBCCMPFieldBridge cmpField = (JDBCCMPFieldBridge) field;

                if (!cmpField.isLoaded(ctx)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Preloading data:" +
                                " entity=" + manager.getEntityBridge().getEntityName() +
                                " pk=" + ctx.getPrimaryKey() +
                                " cmpField=" + cmpField.getFieldName());
                    }

                    // set the value
                    cmpField.setInstanceValue(ctx, value);

                    // mark this field clean as it's value was just loaded
                    cmpField.setClean(ctx);

                    loaded = true;
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("CMPField already loaded:" +
                                " entity=" + manager.getEntityBridge().getEntityName() +
                                " pk=" + ctx.getPrimaryKey() +
                                " cmpField=" + cmpField.getFieldName());
                    }
                }
            } else if (field instanceof JDBCCMRFieldBridge) {
                JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) field;

                if (!cmrField.isLoaded(ctx)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Preloading data:" +
                                " entity=" + manager.getEntityBridge().getEntityName() +
                                " pk=" + ctx.getPrimaryKey() +
                                " cmrField=" + cmrField.getFieldName());
                    }

                    // set the value
                    cmrField.load(ctx, (List) value);

                    // add the loaded list to the related entity's readahead cache
                    JDBCStoreManager relatedManager = (JDBCStoreManager) cmrField.getRelatedCMRField().getManager();
                    ReadAheadCache relatedReadAheadCache =
                            relatedManager.getReadAheadCache();
                    relatedReadAheadCache.addFinderResults(
                            (List) value, cmrField.getReadAhead());

                    if (!loaded) {
                        // this is a hack to fix on-load read-ahead for 1:m relationships
                        if (cmrField.isSingleValued() && onlyOneSingleValuedCMR == null) {
                            onlyOneSingleValuedCMR = cmrField;
                        } else {
                            loaded = true;
                        }
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("CMRField already loaded:" +
                                " entity=" + manager.getEntityBridge().getEntityName() +
                                " pk=" + ctx.getPrimaryKey() +
                                " cmrField=" + cmrField.getFieldName());
                    }
                }
            }
        }

        if (cleanReadAhead) {
            // remove all preload data map as all of the data has been loaded
            manager.removeEntityTxData(new PreloadKey(ctx.getPrimaryKey()));
        }

        return loaded;
    }

    /**
     * Returns the cached value of a CMR field or null if nothing was cached for this field.
     *
     * @param pk       primary key.
     * @param cmrField the field to get the cached value for.
     * @return cached value for the <code>cmrField</code> or null if no value cached.
     */
    public Collection getCachedCMRValue(Object pk, JDBCCMRFieldBridge cmrField) {
        Map preloadDataMap = getPreloadDataMap(pk, true);
        return (Collection) preloadDataMap.get(cmrField);
    }

    /**
     * Add preloaded data for an entity within the scope of a transaction
     */
    public void addPreloadData(Object pk,
                               JDBCFieldBridge field,
                               Object fieldValue) {
        if (field instanceof JDBCCMRFieldBridge) {
            if (fieldValue == null) {
                fieldValue = Collections.EMPTY_LIST;
            } else if (!(fieldValue instanceof Collection)) {
                fieldValue = Collections.singletonList(fieldValue);
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Add preload data:" +
                    " entity=" + manager.getEntityBridge().getEntityName() +
                    " pk=" + pk +
                    " field=" + field.getFieldName());
        }

        // convert null values to a null value standing object
        if (fieldValue == null) {
            fieldValue = NULL_VALUE;
        }

        // store the preloaded data
        Map preloadDataMap = getPreloadDataMap(pk, true);
        Object overridden = preloadDataMap.put(field, fieldValue);

        if (log.isTraceEnabled() && overridden != null) {
            log.trace(
                    "Overriding cached value " + overridden +
                            " with " + (fieldValue == NULL_VALUE ? null : fieldValue) +
                            ". pk=" + pk +
                            ", field=" + field.getFieldName()
            );
        }
    }

    public void removeCachedData(Object primaryKey) {
        if (log.isTraceEnabled()) {
            log.trace("Removing cached data for " + primaryKey);
        }

        Map listMap = getListMap();
        if (listMap == null) {
            // no active tx
            return;
        }

        // remove the preloaded data
        manager.removeEntityTxData(new PreloadKey(primaryKey));

        // if the entity didn't have readahead entry, or it was read-ahead
        // none; return
        EntityMapEntry oldInfo = (EntityMapEntry) listMap.remove(primaryKey);
        if (oldInfo == null || oldInfo.readahead.isNone()) {
            return;
        }

        // check to see if the dereferenced finder result is still referenced
        Iterator iter = listMap.values().iterator();
        while (iter.hasNext()) {
            EntityMapEntry entry = (EntityMapEntry) iter.next();

            // use == because only identity matters here
            if (entry.results == oldInfo.results) {
                // ok it is still referenced
                return;
            }
        }

        // a reference to the old finder set was not found so remove it
        if (log.isTraceEnabled()) {
            log.trace("Removing dereferenced finder results: " +
                    oldInfo.results);
        }
        listCache.remove(oldInfo.results);
    }

    /**
     * Gets the map of preloaded data.
     *
     * @param entityPrimaryKey the primary key of the entity
     * @param create           should a new preload data map be created if one is not found
     * @return the preload data map for null if one is not found
     */
    public Map getPreloadDataMap(Object entityPrimaryKey, boolean create) {
        //
        // Be careful in this code. A soft reference may be cleared at any time,
        // so don't check if a reference has a value and then get that value.
        // Instead get the value and then check if it is null.
        //

        // create a preload key for the entity
        PreloadKey preloadKey = new PreloadKey(entityPrimaryKey);

        // get the soft reference to the preload data map
        SoftReference ref = (SoftReference) manager.getEntityTxData(preloadKey);

        // did we get a reference
        if (ref != null) {
            // get the  map from the reference
            Map preloadDataMap = (Map) ref.get();

            // did we actually get a map? (will be null if it has been GC'd)
            if (preloadDataMap != null) {
                return preloadDataMap;
            }
        }

        //
        // at this point we did not get an existing value
        //
        // if we got a dead reference remove it
        if (ref != null) {
            manager.removeEntityTxData(preloadKey);
        }

        // if we are not creating, we're done
        if (!create) {
            return null;
        }

        // create the new preload data map
        Map preloadDataMap = new HashMap();

        // create new soft reference
        ref = new SoftReference(preloadDataMap);

        // store the reference
        manager.putEntityTxData(preloadKey, ref);

        // return the new preload data map
        return preloadDataMap;
    }

    private Map getListMap() {
        return (Map) listMapTxLocal.get();
    }

    private final class ListCache {
        private TransactionLocal cacheTxLocal;
        private final int max;

        public ListCache(int max) {
            if (max < 0)
                throw MESSAGES.listCacheMaxIsNegative(max);
            this.max = max;
        }

        public void add(List list) {
            if (max == 0) {
                // we're not caching lists, so we're done
                return;
            }

            LinkedList cache = getCache();
            if (cache == null)
                return;
            cache.addFirst(new IdentityObject(list));

            // shrink size to max
            while (cache.size() > max) {
                IdentityObject object = (IdentityObject) cache.removeLast();
                ageOut((List) object.getObject());
            }
        }

        public void promote(List list) {
            if (max == 0) {
                // we're not caching lists, so we're done
                return;
            }

            LinkedList cache = getCache();
            if (cache == null)
                return;

            IdentityObject object = new IdentityObject(list);
            if (cache.remove(object)) {
                // it was in the cache so add it to the front
                cache.addFirst(object);
            }
        }

        public void remove(List list) {
            if (max == 0) {
                // we're not caching lists, so we're done
                return;
            }
            LinkedList cache = getCache();
            if (cache != null)
                cache.remove(new IdentityObject(list));
        }

        public void clear() {
            if (max == 0) {
                // we're not caching lists, so we're done
                return;
            }
        }

        private void ageOut(List list) {
            removeFinderResult(list);
        }

        private LinkedList getCache() {
            return (LinkedList) cacheTxLocal.get();
        }

        public void start() {
            cacheTxLocal = new TransactionLocal(ReadAheadCache.this.manager.getComponent().getTransactionManager()) {
                protected Object initialValue() {
                    return new LinkedList();
                }

                public Transaction getTransaction() {
                    try {
                        return transactionManager.getTransaction();
                    } catch (SystemException e) {
                        throw CmpMessages.MESSAGES.errorGettingCurrentTransaction(e);
                    }
                }
            };
        }
    }

    /**
     * Wraps an entity primary key, so it does not collide with other
     * data stored in the entityTxDataMap.
     */
    private static final class PreloadKey {
        private final Object entityPrimaryKey;

        public PreloadKey(Object entityPrimaryKey) {
            if (entityPrimaryKey == null) {
                throw MESSAGES.entityPrimaryKeyIsNull();
            }
            this.entityPrimaryKey = entityPrimaryKey;
        }

        public boolean equals(Object object) {
            if (object instanceof PreloadKey) {
                PreloadKey preloadKey = (PreloadKey) object;
                return preloadKey.entityPrimaryKey.equals(entityPrimaryKey);
            }
            return false;
        }

        public int hashCode() {
            return entityPrimaryKey.hashCode();
        }

        public String toString() {
            return "PreloadKey: entityId=" + entityPrimaryKey;
        }
    }

    private static final class EntityMapEntry {
        public final int index;
        public final List results;
        public final JDBCReadAheadMetaData readahead;

        private EntityMapEntry(
                int index,
                List results,
                JDBCReadAheadMetaData readahead) {

            this.index = index;
            this.results = results;
            this.readahead = readahead;
        }
    }

    public static final class EntityReadAheadInfo {
        private final List loadKeys;
        private final JDBCReadAheadMetaData readahead;

        private EntityReadAheadInfo(List loadKeys) {
            this(loadKeys, null);
        }

        private EntityReadAheadInfo(List loadKeys, JDBCReadAheadMetaData r) {
            this.loadKeys = loadKeys;
            this.readahead = r;
        }

        public List getLoadKeys() {
            return loadKeys;
        }

        public JDBCReadAheadMetaData getReadAhead() {
            return readahead;
        }
    }

    /**
     * Wraps an Object and does equals/hashCode based on object identity.
     */
    private static final class IdentityObject {
        private final Object object;

        public IdentityObject(Object object) {
            if (object == null) {
                throw MESSAGES.objectIsNull();
            }
            this.object = object;
        }

        public Object getObject() {
            return object;
        }

        public boolean equals(Object object) {
            return this.object == object;
        }

        public int hashCode() {
            return object.hashCode();
        }

        public String toString() {
            return object.toString();
        }
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.OutgoingAttributeGranularitySessionData;

/**
 * Implementation of a clustered session where the replication granularity level is attribute based; that is, we replicate only
 * the dirty attributes.
 * <p/>
 * Note that the isolation level of the cache dictates the concurrency behavior. Also note that session and its associated
 * attributes are stored in different nodes. This will be ok since cache will take care of concurrency. When replicating, we
 * will need to replicate both session and its attributes.
 * </p>
 * @author Ben Wang
 * @author Brian Stansberry
 */
class AttributeBasedClusteredSession extends ClusteredSession<OutgoingAttributeGranularitySessionData> {
    static final long serialVersionUID = -5625209785550936713L;
    /**
     * Descriptive information describing this Session implementation.
     */
    protected static final String info = "AttributeBasedClusteredSession/1.0";

    // Transient map to store attr changes for replication.
    private transient Map<String, Object> attrModifiedMap_ = new HashMap<String, Object>();
    // Transient set to store attr removals for replication
    private transient Set<String> attrRemovedSet_ = new HashSet<String>();

    // ------------------------------------------------------------ Constructors

    public AttributeBasedClusteredSession(ClusteredSessionManager<OutgoingAttributeGranularitySessionData> manager) {
        super(manager);
    }

    // ----------------------------------------------- Overridden Public Methods

    @Override
    public String getInfo() {
        return (info);
    }

    /**
     * Override the superclass to additionally reset this class' fields.
     * <p>
     * <strong>NOTE:</strong> It is not anticipated that this method will be called on a ClusteredSession, but we are overriding
     * the method to be thorough.
     * </p>
     */
    @Override
    public void recycle() {
        super.recycle();

        clearAttrChangedMaps();
    }

    // -------------------------------------------- Overridden Protected Methods

    @Override
    protected OutgoingAttributeGranularitySessionData getOutgoingSessionData() {
        Map<String, Object> modAttrs = null;
        Set<String> removeAttrs = null;
        if (isSessionAttributeMapDirty()) {
            if (attrModifiedMap_.size() > 0) {
                modAttrs = new HashMap<String, Object>(attrModifiedMap_);
            }

            if (attrRemovedSet_.size() > 0) {
                removeAttrs = new HashSet<String>(attrRemovedSet_);
            }

            clearAttrChangedMaps();
        }
        DistributableSessionMetadata metadata = isSessionMetadataDirty() ? getSessionMetadata() : null;
        Long timestamp = modAttrs != null || removeAttrs != null || metadata != null || getMustReplicateTimestamp() ? Long
                .valueOf(getSessionTimestamp()) : null;
        return new OutgoingData(getRealId(), getVersion(), timestamp, metadata, modAttrs, removeAttrs);
    }

    @Override
    protected Object getAttributeInternal(String name) {
        Object result = getAttributesInternal().get(name);

        // Do dirty check even if result is null, as w/ SET_AND_GET null
        // still makes us dirty (ensures timely replication w/o using ACCESS)
        if (isGetDirty(result) && !replicationExcludes.contains(name)) {
            attributeChanged(name, result, false);
        }

        return result;
    }

    @Override
    protected Object removeAttributeInternal(String name, boolean localCall, boolean localOnly) {
        Object result = getAttributesInternal().remove(name);
        if (localCall && !replicationExcludes.contains(name))
            attributeChanged(name, result, true);
        return result;
    }

    @Override
    protected Object setAttributeInternal(String key, Object value) {
        Object old = getAttributesInternal().put(key, value);
        if (!replicationExcludes.contains(key))
            attributeChanged(key, value, false);
        return old;
    }

    // ------------------------------------------------------- Private Methods

    private synchronized void attributeChanged(String key, Object value, boolean removal) {
        if (removal) {
            if (attrModifiedMap_.containsKey(key)) {
                attrModifiedMap_.remove(key);
            }
            attrRemovedSet_.add(key);
        } else {
            if (attrRemovedSet_.contains(key)) {
                attrRemovedSet_.remove(key);
            }
            attrModifiedMap_.put(key, value);
        }
        sessionAttributesDirty();
    }

    private synchronized void clearAttrChangedMaps() {
        attrRemovedSet_.clear();
        attrModifiedMap_.clear();
    }

    // ----------------------------------------------------------------- Classes

    private static class OutgoingData extends OutgoingDistributableSessionDataImpl implements
            OutgoingAttributeGranularitySessionData {
        private final Map<String, Object> modifiedAttributes;
        private final Set<String> removedAttributes;

        public OutgoingData(String realId, int version, Long timestamp, DistributableSessionMetadata metadata,
                Map<String, Object> modifiedAttributes, Set<String> removedAttributes) {
            super(realId, version, timestamp, metadata);
            this.modifiedAttributes = modifiedAttributes;
            this.removedAttributes = removedAttributes;
        }

        @Override
        public Map<String, Object> getModifiedSessionAttributes() {
            return modifiedAttributes;
        }

        @Override
        public Set<String> getRemovedSessionAttributes() {
            return removedAttributes;
        }
    }
}

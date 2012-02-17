/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.web.OutgoingAttributeGranularitySessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.metadata.web.jboss.ReplicationGranularity;

/**
 * Handles session attribute load/store operations for {@link ReplicationGranularity#ATTRIBUTE} distributed session managers.
 *
 * @author Paul Ferraro
 */
public class FineSessionAttributeStorage implements SessionAttributeStorage<OutgoingAttributeGranularitySessionData> {
    private final SessionAttributeMarshaller marshaller;

    public FineSessionAttributeStorage(SessionAttributeMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.infinispan.SessionAttributeStorage#store(java.util.Map, org.jboss.as.clustering.web.OutgoingDistributableSessionData)
     */
    @Override
    public void store(Map<Object, Object> map, OutgoingAttributeGranularitySessionData sessionData) throws IOException {
        Map<String, Object> modified = sessionData.getModifiedSessionAttributes();

        if (modified != null) {
            for (Map.Entry<String, Object> entry : modified.entrySet()) {
                map.put(entry.getKey(), this.marshaller.marshal(entry.getValue()));
            }
        }

        Set<String> removed = sessionData.getRemovedSessionAttributes();

        if (removed != null) {
            for (String attribute : removed) {
                map.remove(attribute);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.infinispan.SessionAttributeStorage#load(java.util.Map)
     */
    @Override
    public Map<String, Object> load(Map<Object, Object> data) throws IOException, ClassNotFoundException {
        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String attribute = (String) key;
                result.put(attribute, this.marshaller.unmarshal(entry.getValue()));
            }
        }

        return result;
    }
}

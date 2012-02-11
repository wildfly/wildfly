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
import java.util.Map;

import org.jboss.as.clustering.web.OutgoingSessionGranularitySessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.metadata.web.jboss.ReplicationGranularity;

/**
 * Handles session attribute load/store operations for {@link ReplicationGranularity#SESSION} distributed session managers.
 *
 * @author Paul Ferraro
 */
public class CoarseSessionAttributeStorage implements SessionAttributeStorage<OutgoingSessionGranularitySessionData> {
    private final SessionAttributeMarshaller marshaller;

    public CoarseSessionAttributeStorage(SessionAttributeMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.infinispan.SessionAttributeStorage#store(java.util.Map, org.jboss.as.clustering.web.OutgoingDistributableSessionData)
     */
    @Override
    public void store(Map<Object, Object> map, OutgoingSessionGranularitySessionData sessionData) throws IOException {
        Map<String, Object> attributes = sessionData.getSessionAttributes();
        if (attributes != null) {
            SessionMapEntry.ATTRIBUTES.put(map, this.marshaller.marshal(attributes));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.infinispan.SessionAttributeStorage#load(java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> load(Map<Object, Object> map) throws IOException, ClassNotFoundException {
        return (Map<String, Object>) this.marshaller.unmarshal(SessionMapEntry.ATTRIBUTES.get(map));
    }
}

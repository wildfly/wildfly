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

import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.metadata.web.jboss.ReplicationGranularity;

import static org.jboss.as.clustering.web.infinispan.InfinispanWebLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.web.infinispan.InfinispanWebMessages.MESSAGES;

/**
 * Default factory for creating strategies for storing session attributes.
 *
 * @author Paul Ferraro
 */
public class SessionAttributeStorageFactoryImpl implements SessionAttributeStorageFactory {

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.infinispan.SessionAttributeStorageFactory#createStorage(org.jboss.metadata.web.jboss.ReplicationGranularity, org.jboss.as.clustering.web.SessionAttributeMarshaller)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends OutgoingDistributableSessionData> SessionAttributeStorage<T> createStorage(ReplicationGranularity granularity, SessionAttributeMarshaller marshaller) {
        switch ((granularity != null) ? granularity : ReplicationGranularity.SESSION) {
            case SESSION: {
                return (SessionAttributeStorage<T>) new CoarseSessionAttributeStorage(marshaller);
            }
            case ATTRIBUTE: {
                return (SessionAttributeStorage<T>) new FineSessionAttributeStorage(marshaller);
            }
            case FIELD: {
                ROOT_LOGGER.deprecatedGranularity(ReplicationGranularity.FIELD.name(), ReplicationGranularity.SESSION.name());
                return this.createStorage(ReplicationGranularity.SESSION, marshaller);
            }
            default: {
                throw MESSAGES.unknownReplicationGranularity(granularity);
            }
        }
    }
}

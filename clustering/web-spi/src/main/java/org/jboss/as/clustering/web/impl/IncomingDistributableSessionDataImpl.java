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

package org.jboss.as.clustering.web.impl;

import java.util.Map;

import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;

import static org.jboss.as.clustering.web.impl.ClusteringWebMessages.MESSAGES;

/**
 * Base implementation of {@link DistributableSessionData}.
 *
 * @author Brian Stansberry
 */
public class IncomingDistributableSessionDataImpl implements IncomingDistributableSessionData {
    private final int version;
    private final long timestamp;
    private final DistributableSessionMetadata metadata;
    private volatile Map<String, Object> attributes;

    public IncomingDistributableSessionDataImpl(Integer version, Long timestamp, DistributableSessionMetadata metadata) {
        this.version = version.intValue();
        this.timestamp = timestamp.longValue();
        this.metadata = metadata;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.IncomingDistributableSessionData#providesSessionAttributes()
     */
    @Override
    public boolean providesSessionAttributes() {
        return this.attributes != null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.IncomingDistributableSessionData#getSessionAttributes()
     */
    @Override
    public Map<String, Object> getSessionAttributes() {
        if (this.attributes == null) {
            throw MESSAGES.sessionAttributesNotConfigured();
        }

        return attributes;
    }

    /**
     * Sets the session attributes.
     *
     * @param attributes a map of session attributes
     */
    public void setSessionAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.IncomingDistributableSessionData#getMetadata()
     */
    @Override
    public DistributableSessionMetadata getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.IncomingDistributableSessionData#getTimestamp()
     */
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.IncomingDistributableSessionData#getVersion()
     */
    @Override
    public int getVersion() {
        return version;
    }
}

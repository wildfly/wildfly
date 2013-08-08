/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * An immutable "snapshot" of a session's meta-data which can be accessed outside the scope of a transaction.
 * @author Paul Ferraro
 */
public class SimpleImmutableSessionMetaData implements ImmutableSessionMetaData {

    private final boolean isNew;
    private final boolean expired;
    private final Date creationTime;
    private final Date lastAccessedTime;
    private final long maxInactiveInterval;

    public SimpleImmutableSessionMetaData(ImmutableSessionMetaData metaData) {
        this.isNew = metaData.isNew();
        this.expired = metaData.isExpired();
        this.creationTime = metaData.getCreationTime();
        this.lastAccessedTime = metaData.getLastAccessedTime();
        this.maxInactiveInterval = metaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @Override
    public boolean isExpired() {
        return this.expired;
    }

    @Override
    public Date getCreationTime() {
        return this.creationTime;
    }

    @Override
    public Date getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public long getMaxInactiveInterval(TimeUnit unit) {
        return unit.convert(this.maxInactiveInterval, TimeUnit.MILLISECONDS);
    }
}
